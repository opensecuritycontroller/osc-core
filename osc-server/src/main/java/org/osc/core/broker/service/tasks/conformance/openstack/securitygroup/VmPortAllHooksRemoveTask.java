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

import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.sdn.NetworkElementImpl;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.util.log.LogProvider;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

/**
 * This task is responsible for removing all the inspection appliances
 * assigned to protected VM port. If the related SDN controller
 * does not support port group it will also remove orphan inspection hooks
 * in the controller.
 */
@Component(service=VmPortAllHooksRemoveTask.class)
public class VmPortAllHooksRemoveTask extends TransactionalTask {

    private final Logger log = LogProvider.getLogger(VmPortAllHooksRemoveTask.class);

    private SecurityGroupMember sgm;
    private VMPort port;
    private String sgmName;
    private SecurityGroupMemberType sgmType;

    @Reference
    private ApiFactoryService apiFactoryService;

    public VmPortAllHooksRemoveTask create(SecurityGroupMember sgm, VMPort port) {
        VmPortAllHooksRemoveTask task = new VmPortAllHooksRemoveTask();
        task.sgm = sgm;
        task.port = port;
        task.sgmType = sgm.getType();
        task.sgmName = sgm.getMemberName();
        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.sgm = em.find(SecurityGroupMember.class, this.sgm.getId());
        this.port = em.find(VMPort.class, this.port.getId());

        this.log.info(String.format("Removing hooks for Stale VM Port with MAC '%s' belonging to %s member '%s'",
                this.port.getMacAddresses(), this.sgmType, this.sgmName));

        this.port.removeAllDais();

        try (SdnRedirectionApi controller = this.apiFactoryService.createNetworkRedirectionApi(this.sgm)) {
            if (this.apiFactoryService.supportsNeutronSFC(this.sgm.getSecurityGroup())) {
                // In case of SFC, removing the flow classifier(Inspection hook) is effectively removing all
                // inspection hooks for that port.
                controller.removeInspectionHook(this.port.getInspectionHookId());
            } else {
                controller.removeAllInspectionHooks(new NetworkElementImpl(this.port));
            }
        }

        OSCEntityManager.update(em, this.port, this.txBroadcastUtil);
    }

    @Override
    public String getName() {
        return String.format("Removing hooks for Stale Port with MAC '%s' belonging to %s member '%s'",
                this.port.getMacAddresses(), this.sgmType, this.sgmName);
    }
}
