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
package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.OpenstackUtil;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.NetworkElement;

public class CreatePortGroupTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(CreatePortGroupTask.class);


    private SecurityGroup securityGroup;

    public CreatePortGroupTask(SecurityGroup sg) {
        this.securityGroup = sg;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.securityGroup = em.find(SecurityGroup.class, this.securityGroup.getId());

        Set<SecurityGroupMember> members = this.securityGroup.getSecurityGroupMembers();
        List<NetworkElement> protectedPorts = new ArrayList<>();

        for (SecurityGroupMember sgm : members) {
            protectedPorts.addAll(OpenstackUtil.getPorts(sgm));
        }
        String domainId = OpenstackUtil.extractDomainId(this.securityGroup.getTenantId(), this.securityGroup.getTenantName(),
                this.securityGroup.getVirtualizationConnector(), protectedPorts);
        if (domainId == null){
            throw new Exception(String.format("A domain was not found for the tenant: '%s' and Security Group: '%s",
                    this.securityGroup.getTenantName(), this.securityGroup.getName()));
        }

        SdnRedirectionApi controller = SdnControllerApiFactory.createNetworkRedirectionApi(
                this.securityGroup.getVirtualizationConnector());
        if (CollectionUtils.isNotEmpty(protectedPorts)) {
            for (NetworkElement vmPort : protectedPorts) {
                ((VMPort) vmPort).setParentId(domainId);
            }
            NetworkElement portGp = controller.registerNetworkElement(protectedPorts);
            if (portGp == null) {
                throw new Exception("RegisterNetworkElement failed to return PortGroup");
            }
            this.securityGroup.setNetworkElementId(portGp.getElementId());
            em.merge(this.securityGroup);

        }
    }

    @Override
    public String getName() {
        return String.format("Create Port Group for security group: %s ", this.securityGroup) ;
    }
}
