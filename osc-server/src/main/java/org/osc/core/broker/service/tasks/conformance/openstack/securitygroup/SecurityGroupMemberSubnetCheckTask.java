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
import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;
import org.osc.core.broker.rest.client.openstack.discovery.VmDiscoveryCache;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNeutron;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = SecurityGroupMemberSubnetCheckTask.class)
public class SecurityGroupMemberSubnetCheckTask extends TransactionalMetaTask {

    @Reference
    SecurityGroupMemberAllHooksRemoveTask securityGroupMemberAllHooksRemoveTask;
    @Reference
    SecurityGroupMemberDeleteTask securityGroupMemberDeleteTask;
    @Reference
    SecurityGroupMemberHookCheckTask securityGroupMemberHookCheckTask;
    @Reference
    SecurityGroupMemberSubnetUpdateTask securityGroupMemberSubnetUpdateTask;

    private TaskGraph tg;
    private SecurityGroupMember sgm;
    private Subnet subnet;
    private VmDiscoveryCache vdc;

    /**
     * Checks the security group member and updates the associated flows
     */
    public SecurityGroupMemberSubnetCheckTask create(SecurityGroupMember sgm, Subnet subnet, VmDiscoveryCache vdc) {
        SecurityGroupMemberSubnetCheckTask task = new SecurityGroupMemberSubnetCheckTask();
        task.sgm = sgm;
        task.subnet = subnet;
        task.vdc = vdc;
        task.securityGroupMemberAllHooksRemoveTask = this.securityGroupMemberAllHooksRemoveTask;
        task.securityGroupMemberDeleteTask = this.securityGroupMemberDeleteTask;
        task.securityGroupMemberHookCheckTask = this.securityGroupMemberHookCheckTask;
        task.securityGroupMemberSubnetUpdateTask = this.securityGroupMemberSubnetUpdateTask;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();
        this.sgm = em.find(SecurityGroupMember.class, this.sgm.getId());
        this.subnet = this.sgm.getSubnet();

        boolean isControllerDefined = this.sgm.getSecurityGroup().getVirtualizationConnector().isControllerDefined();

        SecurityGroup sg = this.sgm.getSecurityGroup();

        Endpoint endPoint = new Endpoint(sg.getVirtualizationConnector(), sg.getProjectName());
        try (Openstack4JNeutron neutron = new Openstack4JNeutron(endPoint)) {
            org.openstack4j.model.network.Subnet subnet = neutron.getSubnetById(this.subnet.getRegion(),
                    this.subnet.getOpenstackId());

            if (subnet == null || this.sgm.getMarkedForDeletion()) {
                if (isControllerDefined) {
                    this.tg.addTask(this.securityGroupMemberAllHooksRemoveTask.create(this.sgm));
                }
                this.tg.appendTask(this.securityGroupMemberDeleteTask.create(this.sgm));
            } else {
                this.tg.addTask(this.securityGroupMemberSubnetUpdateTask.create(this.sgm, this.subnet.getName()));
                if (isControllerDefined) {
                    this.tg.appendTask(this.securityGroupMemberHookCheckTask.create(this.sgm, this.vdc));
                }
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Checking Security Group Member of type '%s' with Name '%s'", this.sgm.getType(),
                this.subnet.getName());
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

}
