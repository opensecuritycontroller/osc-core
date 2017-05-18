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
import org.osc.core.broker.job.TaskOutput;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.sdn.api.VendorTemplateApi;
import org.osgi.service.component.annotations.Component;

@Component
public class RegisterVendorTemplateTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(RegisterVendorTemplateTask.class);

    private VirtualSystem vs;
    private Policy policy;
    private VirtualSystemPolicy vsp;

    @TaskOutput
    public String vendorTemplateId;

    public RegisterVendorTemplateTask create(VirtualSystem vs, Policy policy) {
        RegisterVendorTemplateTask task = new RegisterVendorTemplateTask();
        task.vs = vs;
        task.policy = policy;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    public RegisterVendorTemplateTask create(VirtualSystemPolicy vsp) {
        RegisterVendorTemplateTask task = new RegisterVendorTemplateTask();
        task.vsp = vsp;
        task.vs = vsp.getVirtualSystem();
        task.policy = vsp.getPolicy();
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        LOG.debug("Start excecuting RegisterVendorTemplate Task");

        if (this.vsp == null) {
            this.vs = em.find(VirtualSystem.class, this.vs.getId());
            this.policy = em.find(Policy.class, this.policy.getId());

            this.vsp = new VirtualSystemPolicy(this.vs);
            this.vsp.setPolicy(this.policy);
            OSCEntityManager.create(em, this.vsp, this.txBroadcastUtil);
        } else {
            this.vsp = em.find(VirtualSystemPolicy.class, this.vsp.getId());
        }

        VendorTemplateApi templateApi = VMwareSdnApiFactory.createVendorTemplateApi(this.vsp.getVirtualSystem());
        this.vendorTemplateId = templateApi.createVendorTemplate(this.vsp.getVirtualSystem().getNsxServiceId(), this.vsp.getPolicy().getName(), this.vsp.getPolicy().getId().toString());

        LOG.debug("Update policyVendorTemplateId: " + this.vendorTemplateId);
        this.vsp.setNsxVendorTemplateId(this.vendorTemplateId);
        OSCEntityManager.update(em, this.vsp, this.txBroadcastUtil);
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
