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
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.k8s.PodPort;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.plugin.sdncontroller.NetworkElementImpl;
import org.osc.core.broker.model.plugin.sdncontroller.PodNetworkElementImpl;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.OpenstackUtil;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.NetworkElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = CreatePortGroupTask.class)
public class CreatePortGroupTask extends TransactionalTask {

    @Reference
    private ApiFactoryService apiFactoryService;

    private SecurityGroup securityGroup;

    public CreatePortGroupTask create(SecurityGroup sg) {
        CreatePortGroupTask task = new CreatePortGroupTask();
        task.securityGroup = sg;
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
        String domainId = null;
        if (this.securityGroup.getVirtualizationConnector().getVirtualizationType().isOpenstack()) {
            for (SecurityGroupMember sgm : members) {
                protectedPorts.addAll(OpenstackUtil.getPorts(sgm));
            }
            domainId = OpenstackUtil.extractDomainId(this.securityGroup.getProjectId(), this.securityGroup.getProjectName(),
                    this.securityGroup.getVirtualizationConnector(), protectedPorts);
            if (domainId == null){
                throw new Exception(String.format("A domain was not found for the project: '%s' and Security Group: '%s",
                        this.securityGroup.getProjectName(), this.securityGroup.getName()));
            }

            for (NetworkElement elem : protectedPorts) {
                ((NetworkElementImpl) elem).setParentId(domainId);
            }
        } else {
            for (SecurityGroupMember sgm : members) {
                if (domainId == null && !sgm.getPodPorts().isEmpty()) {
                    domainId = sgm.getPodPorts().iterator().next().getParentId();
                }

                List<PodNetworkElementImpl> podPorts = getPodPorts(sgm);
                for (PodNetworkElementImpl podPort : podPorts) {
                    podPort.setParentId(domainId);
                }

                protectedPorts.addAll(podPorts);
            }
        }

        try (SdnRedirectionApi controller = this.apiFactoryService
                .createNetworkRedirectionApi(this.securityGroup.getVirtualizationConnector())) {
            if (CollectionUtils.isNotEmpty(protectedPorts)) {
                NetworkElement portGp = controller.registerNetworkElement(protectedPorts);
                if (portGp == null) {
                    throw new Exception("RegisterNetworkElement failed to return PortGroup");
                }
                this.securityGroup.setNetworkElementId(portGp.getElementId());
                em.merge(this.securityGroup);
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Create Port Group for security group: %s ", this.securityGroup.getName());
    }

    private static List<PodNetworkElementImpl> getPodPorts(SecurityGroupMember sgm) throws VmidcBrokerValidationException {
        Set<PodPort> ports = sgm.getPodPorts();
        return ports.stream()
                .map(PodNetworkElementImpl::new)
                .collect(Collectors.toList());
    }
}
