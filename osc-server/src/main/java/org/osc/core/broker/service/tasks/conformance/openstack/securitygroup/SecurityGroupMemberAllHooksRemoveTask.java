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

import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.sdn.NetworkElementImpl;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.slf4j.LoggerFactory;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

/**
 * This task is responsible for removing all the inspection appliances
 * assigned to a security group member. If the related SDN controller
 * does not support port group it will also remove orphan inspection hooks
 * in the controller.
 */
@Component(service = SecurityGroupMemberAllHooksRemoveTask.class)
public class SecurityGroupMemberAllHooksRemoveTask extends TransactionalTask {

    private final Logger log = LoggerFactory.getLogger(SecurityGroupMemberAllHooksRemoveTask.class);

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

        Set<VMPort> ports = this.sgm.getVmPorts();

        this.log.info(String.format("Removing Inspection Hooks for stale %s Security Group Member '%s'",
                this.sgm.getType(), this.sgm.getMemberName()));

        SdnRedirectionApi controller = this.apiFactoryService.createNetworkRedirectionApi(this.sgm);

        try {
            for (VMPort port : ports) {
                this.log.info("Deleting orphan inspection ports from member '" + this.sgm.getMemberName()
                        + "' And port: '" + port.getOpenstackId() + "'");

                if (this.apiFactoryService.supportsNeutronSFC(this.sgm.getSecurityGroup())) {
                    // In case of SFC, removing the flow classifier(Inspection hook) is effectively removing all
                    // inspection hooks for that port.
                    controller.removeInspectionHook(port.getInspectionHookId());
                } else {
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
