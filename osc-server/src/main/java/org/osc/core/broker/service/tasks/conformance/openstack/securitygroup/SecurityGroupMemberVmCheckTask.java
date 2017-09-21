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
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.rest.client.openstack.discovery.VmDiscoveryCache;
import org.osc.core.broker.rest.client.openstack.discovery.VmDiscoveryCache.VmInfo;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * If sgm is deleted or is marked for deletion, removes all hooks and deletes the sgm
 * else it updates the member information and updates the hooks. Does not handle deleting of hooks.
 */
@Component(service = SecurityGroupMemberVmCheckTask.class)
public class SecurityGroupMemberVmCheckTask extends TransactionalMetaTask {

    @Reference
    SecurityGroupMemberAllHooksRemoveTask securityGroupMemberAllHooksRemoveTask;

    @Reference
    SecurityGroupMemberDeleteTask securityGroupMemberDeleteTask;

    @Reference
    SecurityGroupMemberHookCheckTask securityGroupMemberHookCheckTask;

    @Reference
    SecurityGroupMemberVmUpdateTask securityGroupMemberVmUpdateTask;

    @Reference
    private ApiFactoryService apiFactoryService;

    private TaskGraph tg;
    private SecurityGroupMember sgm;
    private VM vm;
    private VmDiscoveryCache vdc;

    /**
     * Checks the security group member and updates the associated flows
     */
    public SecurityGroupMemberVmCheckTask create(SecurityGroupMember sgm, VM vm, VmDiscoveryCache vdc) {
        SecurityGroupMemberVmCheckTask task = new SecurityGroupMemberVmCheckTask();
        task.sgm = sgm;
        task.vdc = vdc;
        task.vm = vm;
        task.securityGroupMemberAllHooksRemoveTask = this.securityGroupMemberAllHooksRemoveTask;
        task.securityGroupMemberDeleteTask = this.securityGroupMemberDeleteTask;
        task.securityGroupMemberVmUpdateTask = this.securityGroupMemberVmUpdateTask;
        task.securityGroupMemberHookCheckTask = this.securityGroupMemberHookCheckTask;
        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();
        this.sgm = em.find(SecurityGroupMember.class, this.sgm.getId());
        this.vm = this.sgm.getVm();

        SecurityGroup sg = this.sgm.getSecurityGroup();

        // If port grouping is supported, adding/removing hooks is done at the port group level.
        boolean shouldHandleHooks = sg.getVirtualizationConnector().isControllerDefined()
                && !this.apiFactoryService.supportsPortGroup(sg);

        VmInfo vmInfo = this.vdc.discover(this.vm.getRegion(), this.vm.getOpenstackId());
        if (vmInfo == null || this.sgm.getMarkedForDeletion()) {
            if (shouldHandleHooks) {
                this.tg.addTask(this.securityGroupMemberAllHooksRemoveTask.create(this.sgm));
            }
            this.tg.appendTask(this.securityGroupMemberDeleteTask.create(this.sgm));
        } else {
            this.tg.addTask(this.securityGroupMemberVmUpdateTask.create(this.sgm, vmInfo));
            if (shouldHandleHooks) {
                this.tg.appendTask(this.securityGroupMemberHookCheckTask.create(this.sgm, this.vdc));
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
