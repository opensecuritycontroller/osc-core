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

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.sdn.api.ServiceManagerApi;

public class DeleteServiceManagerTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(DeleteServiceManagerTask.class);

    private VirtualSystem vs;

    public DeleteServiceManagerTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        LOG.debug("Start Executing DeleteServiceManager Task for vs " + this.vs.getId());

        // delete service mgr
        ServiceManagerApi serviceManagerApi = VMwareSdnApiFactory.createServiceManagerApi(this.vs);
        String mgrId = this.vs.getNsxServiceManagerId();
        serviceManagerApi.deleteServiceManager(mgrId);

        LOG.debug("Updating nsx sm " + mgrId + " for VirtualSystem: " + this.vs.getId());
        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());
        this.vs.setNsxServiceManagerId(null);
        this.vs.setNsxVsmUuid(null);
        EntityManager.update(session, this.vs);
    }

    @Override
    public String getName() {
        return "Delete Service Manager '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
