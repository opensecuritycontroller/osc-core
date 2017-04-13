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

import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.TaskInput;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.sdn.api.VendorTemplateApi;

public class UpdateVendorTemplateTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(UpdateVendorTemplateTask.class);

    private VirtualSystemPolicy vsp;
    private String newPolicyName;

    public UpdateVendorTemplateTask(VirtualSystemPolicy vsp, String newPolicyName) {
        this.vsp = vsp;
        this.name = getName();
        this.newPolicyName = newPolicyName;
    }

    @TaskInput
    public String svcId;

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        LOG.info("Start excecuting UpdateVendorTemplate Task");

        this.vsp = em.find(VirtualSystemPolicy.class, this.vsp.getId());
        String templateId = this.vsp.getNsxVendorTemplateId();

        if(templateId != null && !templateId.isEmpty()) {
            VendorTemplateApi templateApi = VMwareSdnApiFactory.createVendorTemplateApi(this.vsp.getVirtualSystem());
            templateApi.updateVendorTemplate(
                    this.vsp.getVirtualSystem().getNsxServiceId(),
                    templateId,
                    this.newPolicyName,
                    this.vsp.getPolicy().getId().toString());
        }
    }

    @Override
    public String getName() {
        return "Updating Policy '" + this.vsp.getPolicy().getName() + "' in Virtual System '"
                + this.vsp.getVirtualSystem().getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vsp.getVirtualSystem());
    }

}
