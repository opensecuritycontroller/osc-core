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
package org.osc.core.broker.service.tasks.conformance.virtualsystem;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.TaskOutput;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.sdn.api.VendorTemplateApi;

public class RegisterVendorTemplateTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(RegisterVendorTemplateTask.class);

    private VirtualSystem vs;
    private Policy policy;
    private VirtualSystemPolicy vsp;

    @TaskOutput
    public String vendorTemplateId;

    public RegisterVendorTemplateTask(VirtualSystem vs, Policy policy) {
        this.vs = vs;
        this.policy = policy;
        this.name = getName();
    }

    public RegisterVendorTemplateTask(VirtualSystemPolicy vsp) {
        this.vsp = vsp;
        this.vs = vsp.getVirtualSystem();
        this.policy = vsp.getPolicy();
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        LOG.debug("Start excecuting RegisterVendorTemplate Task");

        if (this.vsp == null) {
            this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());
            this.policy = (Policy) session.get(Policy.class, this.policy.getId());

            this.vsp = new VirtualSystemPolicy(this.vs);
            this.vsp.setPolicy(this.policy);
            EntityManager.create(session, this.vsp);
        } else {
            this.vsp = (VirtualSystemPolicy) session.get(VirtualSystemPolicy.class, this.vsp.getId());
        }

        VendorTemplateApi templateApi = VMwareSdnApiFactory.createVendorTemplateApi(this.vsp.getVirtualSystem());
        this.vendorTemplateId = templateApi.createVendorTemplate(this.vsp.getVirtualSystem().getNsxServiceId(), this.vsp.getPolicy().getName(), this.vsp.getPolicy().getId().toString());

        LOG.debug("Update policyVendorTemplateId: " + this.vendorTemplateId);
        this.vsp.setNsxVendorTemplateId(this.vendorTemplateId);
        EntityManager.update(session, this.vsp);
    }

    @Override
    public String getName() {
        return "Creating Policy '" + this.policy.getName() + "' in '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
