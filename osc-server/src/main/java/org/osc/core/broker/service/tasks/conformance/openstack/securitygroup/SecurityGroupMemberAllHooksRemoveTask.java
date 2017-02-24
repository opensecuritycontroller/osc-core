/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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

import org.hibernate.Session;
import org.jboss.logging.Logger;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.openstack.Network;
import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.controller.api.SdnControllerApi;

class SecurityGroupMemberAllHooksRemoveTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(SecurityGroupMemberAllHooksRemoveTask.class);

    private SecurityGroupMember sgm;

    public SecurityGroupMemberAllHooksRemoveTask(SecurityGroupMember sgm) {
        this.sgm = sgm;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.sgm = (SecurityGroupMember) session.get(SecurityGroupMember.class, this.sgm.getId());

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

        SdnControllerApi controller = SdnControllerApiFactory.createNetworkControllerApi(this.sgm);
        try {
            for (VMPort port : ports) {
                this.log.info("Deleting Zombie Inspection Hooks for member '" + this.sgm.getMemberName()
                        + "' And port: '" + port.getElementId() + "'");
                controller.removeAllInspectionHooks(port);
                port.removeAllDais();
                EntityManager.update(session, port);
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
