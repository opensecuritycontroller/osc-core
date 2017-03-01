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

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.TaskInput;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.sdn.api.VendorTemplateApi;

public class RemoveVendorTemplateTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(RemoveVendorTemplateTask.class);

    private VirtualSystemPolicy vsp;

    public RemoveVendorTemplateTask(VirtualSystemPolicy vsp) {
        this.vsp = vsp;
        this.name = getName();
    }

    @TaskInput
    public String svcId;

    @Override
    public void executeTransaction(Session session) throws Exception {

        log.debug("Start excecuting RemoveVendorTemplate Task");

        this.vsp = (VirtualSystemPolicy) session.get(VirtualSystemPolicy.class, this.vsp.getId());

        VendorTemplateApi templateApi = VMwareSdnApiFactory.createVendorTemplateApi(this.vsp.getVirtualSystem());
        templateApi.deleteVendorTemplate(
                this.vsp.getVirtualSystem().getNsxServiceId(),
                this.vsp.getNsxVendorTemplateId(),
                this.vsp.getPolicy().getId().toString());

        EntityManager.delete(session, this.vsp);

        // If we've removed the last virtual system policies,
        // we can now delete the policy.
        if (this.vsp.getPolicy().getMarkedForDeletion()) {
            EntityManager<VirtualSystemPolicy> em = new EntityManager<VirtualSystemPolicy>(VirtualSystemPolicy.class,
                    session);
            List<VirtualSystemPolicy> vsps = em.listByFieldName("policy", this.vsp.getPolicy());
            if (vsps == null || vsps.isEmpty()) {
                log.info("Deleting policy '" + this.vsp.getPolicy().getName() + "'");
                EntityManager.delete(session, this.vsp.getPolicy());
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
