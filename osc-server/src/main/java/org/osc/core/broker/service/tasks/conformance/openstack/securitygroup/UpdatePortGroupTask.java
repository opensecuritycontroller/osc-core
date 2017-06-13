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

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.plugin.sdncontroller.NetworkElementImpl;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.OpenstackUtil;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.element.PortGroup;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.NetworkElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service=UpdatePortGroupTask.class)
public class UpdatePortGroupTask  extends TransactionalTask{
    private static final Logger LOG = Logger.getLogger(UpdatePortGroupTask.class);

    @Reference
    private ApiFactoryService apiFactoryService;

    private SecurityGroup securityGroup;
    private PortGroup portGroup;

    public UpdatePortGroupTask create(SecurityGroup sg, PortGroup portGroup) {
        UpdatePortGroupTask task = new UpdatePortGroupTask();
        task.securityGroup = sg;
        task.portGroup = portGroup;
        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
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
            throw new Exception(String.format("Failed to retrieve domainId for given tenant: '%s' and Security Group: '%s",
                    this.securityGroup.getTenantName(), this.securityGroup.getName()));
        }
        this.portGroup.setParentId(domainId);
        for (NetworkElement elem : protectedPorts) {
            ((NetworkElementImpl) elem).setParentId(domainId);
        }
        SdnRedirectionApi controller = this.apiFactoryService.createNetworkRedirectionApi(
                this.securityGroup.getVirtualizationConnector());
        NetworkElement portGrp = controller.updateNetworkElement(this.portGroup, protectedPorts);
        if (portGrp == null) {
            throw new Exception(String.format("Failed to update Port Group : '%s'", this.portGroup.getElementId()));
        }
        if (!portGrp.getElementId().equals(this.portGroup.getElementId())) {
            //portGroup was deleted outside OSC, recreated portGroup above
            this.securityGroup.setNetworkElementId(portGrp.getElementId());
            em.merge(this.securityGroup);
        }
    }

    @Override
    public String getName() {
        return String.format("Update Port Group for security group: %s ", this.securityGroup.getName());
    }
}

