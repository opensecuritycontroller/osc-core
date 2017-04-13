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

import javax.persistence.EntityManager;

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.rest.client.openstack.discovery.VmDiscoveryCache;
import org.osc.core.broker.rest.client.openstack.discovery.VmDiscoveryCache.VmInfo;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;

class SecurityGroupMemberVmCheckTask extends TransactionalMetaTask {

    private TaskGraph tg;
    private SecurityGroupMember sgm;
    private VM vm;
    private final VmDiscoveryCache vdc;

    /**
     * Checks the security group member and updates the associated flows
     */
    public SecurityGroupMemberVmCheckTask(SecurityGroupMember sgm, VM vm, VmDiscoveryCache vdc) {
        this.sgm = sgm;
        this.vdc = vdc;
        this.vm = vm;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();
        this.sgm = em.find(SecurityGroupMember.class, this.sgm.getId());
        this.vm = this.sgm.getVm();

        boolean isControllerDefined = this.sgm.getSecurityGroup().getVirtualizationConnector().isControllerDefined();

        VmInfo vmInfo = this.vdc.discover(this.vm.getRegion(), this.vm.getOpenstackId());
        if (vmInfo == null || this.sgm.getMarkedForDeletion()) {
            if (isControllerDefined) {
                this.tg.addTask(new SecurityGroupMemberAllHooksRemoveTask(this.sgm));
            }
            this.tg.appendTask(new SecurityGroupMemberDeleteTask(this.sgm));
        } else {
            this.tg.addTask(new SecurityGroupMemberVmUpdateTask(this.sgm, vmInfo));
            if (isControllerDefined) {
                this.tg.appendTask(new SecurityGroupMemberHookCheckTask(this.sgm, this.vdc));
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Checking Security Group Member of type '%s' with Name '%s'", this.sgm.getType(),
                this.vm.getName());
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

}
