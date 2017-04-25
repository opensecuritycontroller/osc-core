/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.service.openstack;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.jclouds.openstack.neutron.v2.domain.Network;
import org.jclouds.openstack.neutron.v2.domain.Subnet;
import org.jclouds.openstack.v2_0.domain.Resource;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNeutron;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.dto.SecurityGroupMemberItemDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.request.ListOpenstackMembersRequest;
import org.osc.core.broker.service.response.ListResponse;

/**
 * Lists servers based on the openstack request. The Parent ID is assumed to be of the VC
 *
 * The ID is assumed to be the id of a Security group so the server list filters out any existing members of the
 * security group.
 * If the id is not set, all servers from that VC are listed
 */
public class ListOpenstackMembersService extends
        ServiceDispatcher<ListOpenstackMembersRequest, ListResponse<SecurityGroupMemberItemDto>> {

    private VirtualizationConnector vc;

    @Override
    public ListResponse<SecurityGroupMemberItemDto> exec(ListOpenstackMembersRequest request, EntityManager em)
            throws Exception {

        OSCEntityManager<VirtualizationConnector> emgr = new OSCEntityManager<>(VirtualizationConnector.class, em);
        this.vc = emgr.findByPrimaryKey(request.getParentId());
        List<String> existingMemberIds = new ArrayList<>();
        // If current selected members is set to null, assume this is first load and populate existing member ids from
        // DB
        if (request.getCurrentSelectedMembers() == null && request.getId() != null) {
            SecurityGroup sg = SecurityGroupEntityMgr.findById(em, request.getId());
            for (SecurityGroupMember sgm : sg.getSecurityGroupMembers()) {
                if (!sgm.getMarkedForDeletion()) {
                    existingMemberIds.add(getMemberOpenstackId(sgm));
                }
            }
        } else if (request.getCurrentSelectedMembers() != null) {
            for (SecurityGroupMemberItemDto sgmDto : request.getCurrentSelectedMembers()) {
                existingMemberIds.add(sgmDto.getOpenstackId());
            }
        }

        List<SecurityGroupMemberItemDto> openstackMemberList = new ArrayList<>();
        String region = request.getRegion();

        if (request.getType() == SecurityGroupMemberType.VM) {

            List<String> existingSvaOsIds = DistributedApplianceInstanceEntityMgr.listOsServerIdByVcId(em,
                    this.vc.getId());
            existingMemberIds.addAll(existingSvaOsIds);

            JCloudNova nova = null;
            try {
                nova = new JCloudNova(new Endpoint(this.vc, request.getTenantName()));

                for (Resource vmResource : nova.listServers(region)) {
                    if (!existingMemberIds.contains(vmResource.getId())) {
                        openstackMemberList.add(new SecurityGroupMemberItemDto(region, vmResource.getName(), vmResource
                                .getId(), SecurityGroupMemberType.VM, false));
                    }
                }

            } finally {
                if (nova != null) {
                    nova.close();
                }
            }
        } else if (request.getType() == SecurityGroupMemberType.NETWORK) {
            JCloudNeutron neutronApi = new JCloudNeutron(new Endpoint(this.vc, request.getTenantName()));
            try {
                List<Network> tenantNetworks = neutronApi.listNetworkByTenant(request.getRegion(),
                        request.getTenantId());
                for (Network tenantNetwork : tenantNetworks) {
                    if (!existingMemberIds.contains(tenantNetwork.getId())) {
                        openstackMemberList.add(new SecurityGroupMemberItemDto(region, tenantNetwork.getName(),
                                tenantNetwork.getId(), SecurityGroupMemberType.NETWORK, false));
                    }
                }

            } finally {
                if (neutronApi != null) {
                    neutronApi.close();
                }
            }
        } else if (request.getType() == SecurityGroupMemberType.SUBNET) {
            JCloudNeutron neutronApi = new JCloudNeutron(new Endpoint(this.vc, request.getTenantName()));
            try {
                List<Subnet> tenantSubnets = neutronApi.listSubnetByTenant(request.getRegion(), request.getTenantId());
                for (Subnet subnet : tenantSubnets) {
                    if (!existingMemberIds.contains(subnet.getId())) {
                        openstackMemberList.add(new SecurityGroupMemberItemDto(request.getRegion(),
                                createSubnetNetworkName(subnet, request.getRegion(), neutronApi), subnet.getId(),
                                SecurityGroupMemberType.SUBNET, false, subnet.getNetworkId()));
                    }
                }

            } finally {
                if (neutronApi != null) {
                    neutronApi.close();
                }
            }
        }

        ListResponse<SecurityGroupMemberItemDto> response = new ListResponse<>();
        response.setList(openstackMemberList);
        return response;

    }

    private String getMemberOpenstackId(SecurityGroupMember sgm) throws VmidcBrokerValidationException {
        switch (sgm.getType()) {
        case VM:
            return sgm.getVm().getOpenstackId();
        case NETWORK:
            return sgm.getNetwork().getOpenstackId();
        case SUBNET:
            return sgm.getSubnet().getOpenstackId();
        default:
            throw new VmidcBrokerValidationException("Region is not applicable for Members of type '" + sgm.getType() + "'");
        }
    }

    private String createSubnetNetworkName(Subnet subnet, String region, JCloudNeutron neutronApi) {
        Network subnetNetwork = neutronApi.getNetworkById(region, subnet.getNetworkId());
        return subnet.getName() + " (" + subnetNetwork.getName() + ")";
    }
}
