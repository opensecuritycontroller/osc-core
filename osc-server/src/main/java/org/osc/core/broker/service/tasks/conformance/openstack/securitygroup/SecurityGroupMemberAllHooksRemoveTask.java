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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.openstack.Network;
import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.plugin.sdncontroller.NetworkElementImpl;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This task is responsible for removing all the inspection appliances
 * assigned to a security group member. If the related SDN controller
 * does not support port group it will also remove orphan inspection hooks
 * in the controller.
 */
@Component(service = SecurityGroupMemberAllHooksRemoveTask.class)
public class SecurityGroupMemberAllHooksRemoveTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(SecurityGroupMemberAllHooksRemoveTask.class);

    private SecurityGroupMember sgm;

    @Reference
    private ApiFactoryService apiFactoryService;

    public SecurityGroupMemberAllHooksRemoveTask create(SecurityGroupMember sgm) {
        SecurityGroupMemberAllHooksRemoveTask task = new SecurityGroupMemberAllHooksRemoveTask();
        task.sgm = sgm;
        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.sgm = em.find(SecurityGroupMember.class, this.sgm.getId());

        Set<VMPort> ports = new HashSet<>();

        if (this.sgm.getType() == SecurityGroupMemberType.VM) {
            VM vm = this.sgm.getVm();
            this.log.info(String.format("Removing Inspection Hooks for stale Security Group Member VM '%s'",
                    vm.getName()));
            ports = vm.getPorts();
        } else if (this.sgm.getType() == SecurityGroupMemberType.NETWORK) {
            Network network = this.sgm.getNetwork();
            this.log.info(String.format("Removing Inspection Hooks for stale Security Group Member Network '%s'",
                    network.getName()));
            ports = network.getPorts();
        } else if (this.sgm.getType() == SecurityGroupMemberType.SUBNET) {
            Subnet subnet = this.sgm.getSubnet();
            this.log.info(String.format("Removing Inspection Hooks for stale Security Group Member Subnet '%s'",
                    subnet.getName()));
            ports = subnet.getPorts();
        }

        SdnRedirectionApi controller = this.apiFactoryService.createNetworkRedirectionApi(this.sgm);

        try {
            for (VMPort port : ports) {
                this.log.info("Deleting orphan inspection ports from member '" + this.sgm.getMemberName()
                        + "' And port: '" + port.getElementId() + "'");

                // If port group is not supported also remove the inspection hooks from the controller.
                if (!this.apiFactoryService.supportsPortGroup(this.sgm.getSecurityGroup())) {
                controller.removeAllInspectionHooks(new NetworkElementImpl(port));
                }

                port.removeAllDais();
                OSCEntityManager.update(em, port, this.txBroadcastUtil);
            }
        } finally {
            controller.close();
        }
    }

    @Override
    public String getName() {
        return String.format("Removing Inspection Hooks for stale %s Security Group Member '%s'", this.sgm.getType(),
                this.sgm.getMemberName());
    }

}
