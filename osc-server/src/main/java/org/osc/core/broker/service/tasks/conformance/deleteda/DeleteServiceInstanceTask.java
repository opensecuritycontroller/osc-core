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
package org.osc.core.broker.service.tasks.conformance.deleteda;

import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.sdn.api.ServiceInstanceApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = DeleteServiceInstanceTask.class)
public class DeleteServiceInstanceTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(DeleteServiceInstanceTask.class);

    @Reference
    private ApiFactoryService apiFactoryService;

    private VirtualSystem vs;

    public DeleteServiceInstanceTask create(VirtualSystem vs) {
        DeleteServiceInstanceTask task = new DeleteServiceInstanceTask();
        task.vs = vs;
        task.name = task.getName();
        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        LOG.debug("Start executing DeleteServiceInstance task");

        String siId = this.vs.getNsxServiceInstanceId();
        // delete service instance
        ServiceInstanceApi serviceInstanceApi = this.apiFactoryService.createServiceInstanceApi(this.vs);
        serviceInstanceApi.deleteServiceInstance(this.vs.getNsxServiceInstanceId(), this.vs.getNsxServiceId());

        LOG.debug("Updating nsx si " + siId + " for VirtualSystem: " + this.vs.getId());
        this.vs = em.find(VirtualSystem.class, this.vs.getId());
        this.vs.setNsxServiceInstanceId(null);
        OSCEntityManager.update(em, this.vs, this.txBroadcastUtil);
    }

    @Override
    public String getName() {
        return "Delete Service Instance '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
