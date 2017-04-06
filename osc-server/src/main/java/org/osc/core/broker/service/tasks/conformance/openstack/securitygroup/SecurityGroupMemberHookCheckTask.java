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

import org.jboss.logging.Logger;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.rest.client.openstack.discovery.VmDiscoveryCache;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;

/**
 * This task is responsible for checking the conformance of the inspection appliances
 * assigned to a security group member. If the related SDN controller
 * does not support port group it will also create the inspection hook for each VM port
 * of the security group member.
 */
// TODO emanoel: Consider renaming the task to SecurityGroupMemberCheckAppliancesTask since
// it does more than checking the inspection hook.
class SecurityGroupMemberHookCheckTask extends TransactionalMetaTask {

    private final Logger log = Logger.getLogger(SecurityGroupMemberHookCheckTask.class);

    private TaskGraph tg;
    private SecurityGroupMember sgm;
    private final VmDiscoveryCache vdc;

    public SecurityGroupMemberHookCheckTask(SecurityGroupMember sgm, VmDiscoveryCache vdc) {
        this.sgm = sgm;
        this.vdc = vdc;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();
        this.sgm = em.find(SecurityGroupMember.class, this.sgm.getId());

        SecurityGroup sg = this.sgm.getSecurityGroup();

        this.log.info("Checking Inspection Hooks for Security group Member: " + this.sgm.getMemberName());

        Set<VMPort> ports = new HashSet<>();

        if (this.sgm.getType() == SecurityGroupMemberType.VM) {
            ports = this.sgm.getVm().getPorts();
        } else if (this.sgm.getType() == SecurityGroupMemberType.NETWORK) {
            ports = this.sgm.getNetwork().getPorts();
        } else if (this.sgm.getType() == SecurityGroupMemberType.SUBNET) {
            ports = this.sgm.getSubnet().getPorts();
        }

        for (VMPort port : ports) {
            if (port.getMarkedForDeletion()) {
                this.tg.appendTask(new VmPortAllHooksRemoveTask(this.sgm, port));
                this.tg.appendTask(new VmPortDeleteFromDbTask(this.sgm, port));
            } else {
                for (SecurityGroupInterface sgi : sg.getSecurityGroupInterfaces()) {
                    if (!sgi.getMarkedForDeletion() && !SdnControllerApiFactory.supportsPortGroup(this.sgm.getSecurityGroup())) {
                        this.tg.appendTask(new VmPortHookCheckTask(this.sgm, sgi, port, this.vdc),
                                TaskGuard.ALL_PREDECESSORS_COMPLETED);
                    }
                }
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Checking Inspection hooks for %s Security Group Member '%s'", this.sgm.getType(),
                this.sgm.getMemberName());
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

}
