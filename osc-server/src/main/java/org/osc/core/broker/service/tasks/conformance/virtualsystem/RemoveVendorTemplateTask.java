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
package org.osc.core.broker.service.tasks.conformance.virtualsystem;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.TaskInput;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.sdn.api.VendorTemplateApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service=RemoveVendorTemplateTask.class)
public class RemoveVendorTemplateTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(RemoveVendorTemplateTask.class);

    @Reference
    private ApiFactoryService apiFactoryService;

    private VirtualSystemPolicy vsp;

    public RemoveVendorTemplateTask create(VirtualSystemPolicy vsp) {
        RemoveVendorTemplateTask task = new RemoveVendorTemplateTask();
        task.vsp = vsp;
        task.name = getName();
        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @TaskInput
    public String svcId;

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        log.debug("Start excecuting RemoveVendorTemplate Task");

        this.vsp = em.find(VirtualSystemPolicy.class, this.vsp.getId());

        VendorTemplateApi templateApi = this.apiFactoryService.createVendorTemplateApi(this.vsp.getVirtualSystem());
        templateApi.deleteVendorTemplate(
                this.vsp.getVirtualSystem().getNsxServiceId(),
                this.vsp.getNsxVendorTemplateId(),
                this.vsp.getPolicy().getId().toString());

        OSCEntityManager.delete(em, this.vsp, this.txBroadcastUtil);

        // If we've removed the last virtual system policies,
        // we can now delete the policy.
        if (this.vsp.getPolicy().getMarkedForDeletion()) {
            OSCEntityManager<VirtualSystemPolicy> oscEm = new OSCEntityManager<VirtualSystemPolicy>(VirtualSystemPolicy.class,
                    em, this.txBroadcastUtil);
            List<VirtualSystemPolicy> vsps = oscEm.listByFieldName("policy", this.vsp.getPolicy());
            if (vsps == null || vsps.isEmpty()) {
                log.info("Deleting policy '" + this.vsp.getPolicy().getName() + "'");
                OSCEntityManager.delete(em, this.vsp.getPolicy(), this.txBroadcastUtil);
            }
        }
    }

    @Override
    public String getName() {
        return "Delete Policy '" + this.vsp.getPolicy().getName() + "' from '"
                + this.vsp.getVirtualSystem().getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vsp.getVirtualSystem());
    }

}
