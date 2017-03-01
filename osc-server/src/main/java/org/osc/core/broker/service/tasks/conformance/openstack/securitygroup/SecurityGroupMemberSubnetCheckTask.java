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

import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;
import org.osc.core.broker.rest.client.openstack.discovery.VmDiscoveryCache;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNeutron;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;

class SecurityGroupMemberSubnetCheckTask extends TransactionalMetaTask {

    private TaskGraph tg;
    private SecurityGroupMember sgm;
    private Subnet subnet;
    private final VmDiscoveryCache vdc;

    /**
     * Checks the security group member and updates the associated flows
     */
    public SecurityGroupMemberSubnetCheckTask(SecurityGroupMember sgm, Subnet subnet, VmDiscoveryCache vdc) {
        this.sgm = sgm;
        this.subnet = subnet;
        this.vdc = vdc;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.tg = new TaskGraph();
        this.sgm = (SecurityGroupMember) session.get(SecurityGroupMember.class, this.sgm.getId());
        this.subnet = this.sgm.getSubnet();

        boolean isControllerDefined = this.sgm.getSecurityGroup().getVirtualizationConnector().isControllerDefined();

        SecurityGroup sg = this.sgm.getSecurityGroup();

        JCloudNeutron neutron = null;

        try {
            neutron = new JCloudNeutron(new Endpoint(sg.getVirtualizationConnector(), sg.getTenantName()));
            org.jclouds.openstack.neutron.v2.domain.Subnet subnet = neutron.getSubnetById(this.subnet.getRegion(),
                    this.subnet.getOpenstackId());

            if (subnet == null || this.sgm.getMarkedForDeletion()) {
                if (isControllerDefined) {
                    this.tg.addTask(new SecurityGroupMemberAllHooksRemoveTask(this.sgm));
                }
                this.tg.appendTask(new SecurityGroupMemberDeleteTask(this.sgm));
            } else {
                this.tg.addTask(new SecurityGroupMemberSubnetUpdateTask(this.sgm, this.subnet.getName()));
                if (isControllerDefined) {
                	this.tg.appendTask(new SecurityGroupMemberHookCheckTask(this.sgm, this.vdc));
                }
            }
        } finally {
            if (neutron != null) {
                neutron.close();
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
