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
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.sdn.api.ServiceProfileApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service=DeleteDefaultServiceProfileTask.class)
public class DeleteDefaultServiceProfileTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(DeleteDefaultServiceProfileTask.class);

    @Reference
    private ApiFactoryService apiFactoryService;

    private VirtualSystemPolicy vsp;

    public DeleteDefaultServiceProfileTask create(VirtualSystemPolicy vsp) {
        DeleteDefaultServiceProfileTask task = new DeleteDefaultServiceProfileTask();
        task.vsp = vsp;
        task.name = task.getName();
        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        log.debug("Start excecuting DeleteDefaultServiceProfileTask");

        this.vsp = em.find(VirtualSystemPolicy.class, this.vsp.getId());
        ServiceProfileApi serviceProfileApi = this.apiFactoryService.createServiceProfileApi(this.vsp.getVirtualSystem());
        serviceProfileApi.deleteServiceProfile(this.vsp.getVirtualSystem().getNsxServiceId(), this.vsp.getNsxVendorTemplateId());
        log.debug("Deleted service profile of the service: " + this.vsp.getVirtualSystem().getNsxServiceId() + " for vendor template: " + this.vsp.getNsxVendorTemplateId());
    }

    @Override
    public String getName() {
        return "Delete default Profile for Policy '" + this.vsp.getPolicy().getName() + "' in '"
                + this.vsp.getVirtualSystem().getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vsp.getVirtualSystem());
    }

}
