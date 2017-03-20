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
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.Network;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.rest.client.openstack.discovery.VmDiscoveryCache;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNeutron;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.sdk.controller.api.SdnControllerApi;

class SecurityGroupMemberNetworkCheckTask extends TransactionalMetaTask {

    private TaskGraph tg;
    private SecurityGroupMember sgm;
    private Network network;
    private final VmDiscoveryCache vdc;
    /**
     * Checks the security group member and updates the associated flows
     */
    public SecurityGroupMemberNetworkCheckTask(SecurityGroupMember sgm, Network network, VmDiscoveryCache vdc) {
        this.sgm = sgm;
        this.network = network;
        this.vdc = vdc;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.tg = new TaskGraph();
        this.sgm = session.get(SecurityGroupMember.class, this.sgm.getId());
        this.network = this.sgm.getNetwork();

        boolean isControllerDefined = this.sgm.getSecurityGroup().getVirtualizationConnector().isControllerDefined();

        SecurityGroup sg = this.sgm.getSecurityGroup();
        VirtualizationConnector vc = sg.getVirtualizationConnector();

        try (SdnControllerApi controller = SdnControllerApiFactory.createNetworkControllerApi(vc.getControllerType());
                JCloudNeutron neutron = new JCloudNeutron(new Endpoint(sg.getVirtualizationConnector(), sg.getTenantName()));){
            boolean isPortGroupSupported = SdnControllerApiFactory.supportsPortGroup(sg);
            org.jclouds.openstack.neutron.v2.domain.Network neutronNetwork = neutron.getNetworkById(
                    this.network.getRegion(), this.network.getOpenstackId());

            if (neutronNetwork == null || this.sgm.getMarkedForDeletion()) {
                if (isControllerDefined && !isPortGroupSupported) {
                    this.tg.addTask(new SecurityGroupMemberAllHooksRemoveTask(this.sgm));
                }
                this.tg.appendTask(new SecurityGroupMemberDeleteTask(this.sgm));
            } else {
                this.tg.addTask(new SecurityGroupMemberNetworkUpdateTask(this.sgm, neutronNetwork.getName()));
                if (isControllerDefined && !isPortGroupSupported) {
                	this.tg.appendTask(new SecurityGroupMemberHookCheckTask(this.sgm, this.vdc));
                }
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Checking Security Group Member of type '%s' with Name '%s'", this.sgm.getType(),
                this.network.getName());
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

}
