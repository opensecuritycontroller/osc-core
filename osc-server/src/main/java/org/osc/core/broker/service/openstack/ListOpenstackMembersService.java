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

import org.openstack4j.model.compute.Server;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Subnet;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNeutron;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNova;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.ListOpenstackMembersServiceApi;
import org.osc.core.broker.service.dto.SecurityGroupMemberItemDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.request.ListOpenstackMembersRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osgi.service.component.annotations.Component;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Lists servers based on the openstack request. The Parent ID is assumed to be of the VC
 * <p>
 * The ID is assumed to be the id of a Security group so the server list filters out any existing members of the
 * security group.
 * If the id is not set, all servers from that VC are listed
 */
@Component
public class ListOpenstackMembersService
        extends ServiceDispatcher<ListOpenstackMembersRequest, ListResponse<SecurityGroupMemberItemDto>>
        implements ListOpenstackMembersServiceApi {


    @Override
    public ListResponse<SecurityGroupMemberItemDto> exec(ListOpenstackMembersRequest request, EntityManager em)
            throws Exception {

        OSCEntityManager<VirtualizationConnector> emgr = new OSCEntityManager<>(VirtualizationConnector.class, em, this.txBroadcastUtil);
        VirtualizationConnector vc = emgr.findByPrimaryKey(request.getParentId());
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

        if (SecurityGroupMemberType.fromText(request.getType()) == SecurityGroupMemberType.VM) {

            List<String> existingSvaOsIds = DistributedApplianceInstanceEntityMgr.listOsServerIdByVcId(em, vc.getId());
            existingMemberIds.addAll(existingSvaOsIds);

            try (Openstack4JNova nova = new Openstack4JNova(new Endpoint(vc, request.getTenantName()))) {
                for (Server vmResource : nova.listServers(region)) {
                    if (!existingMemberIds.contains(vmResource.getId())) {
                        openstackMemberList.add(new SecurityGroupMemberItemDto(region, vmResource.getName(), vmResource
                                .getId(), SecurityGroupMemberType.VM.toString(), false));
                    }
                }
            }

        } else if (SecurityGroupMemberType.fromText(request.getType()) == SecurityGroupMemberType.NETWORK) {
            try (Openstack4JNeutron neutronApi = new Openstack4JNeutron(new Endpoint(vc, request.getTenantName()))) {
                List<Network> tenantNetworks = neutronApi.listNetworkByTenant(request.getRegion(), request.getTenantId());
                for (Network tenantNetwork : tenantNetworks) {
                    if (!existingMemberIds.contains(tenantNetwork.getId())) {
                        openstackMemberList.add(new SecurityGroupMemberItemDto(region, tenantNetwork.getName(),
                                tenantNetwork.getId(), SecurityGroupMemberType.NETWORK.toString(), false));
                    }
                }
            }
        } else if (SecurityGroupMemberType.fromText(request.getType()) == SecurityGroupMemberType.SUBNET) {
            try (Openstack4JNeutron neutronApi = new Openstack4JNeutron(new Endpoint(vc, request.getTenantName()))) {
                List<Subnet> tenantSubnets = neutronApi.listSubnetByTenant(request.getRegion(), request.getTenantId());
                for (Subnet subnet : tenantSubnets) {
                    if (!existingMemberIds.contains(subnet.getId())) {
                        openstackMemberList.add(new SecurityGroupMemberItemDto(request.getRegion(),
                                createSubnetNetworkName(subnet, request.getRegion(), neutronApi), subnet.getId(),
                                SecurityGroupMemberType.SUBNET.toString(), false, subnet.getNetworkId()));
                    }
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

    private String createSubnetNetworkName(Subnet subnet, String region, Openstack4JNeutron neutronApi) {
        Network subnetNetwork = neutronApi.getNetworkById(region, subnet.getNetworkId());
        return subnet.getName() + " (" + subnetNetwork.getName() + ")";
    }
}
