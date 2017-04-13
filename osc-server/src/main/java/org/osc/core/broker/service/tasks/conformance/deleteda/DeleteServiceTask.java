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
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.sdn.api.ServiceApi;
import org.osc.sdk.sdn.element.ServiceElement;

public class DeleteServiceTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(DeleteServiceTask.class);

    private VirtualSystem vs;

    public DeleteServiceTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        LOG.debug("Start Executing DeleteServiceTask Task for vs " + this.vs.getId());

        // delete service

        String sId = this.vs.getNsxServiceId();

        ServiceApi serviceApi = VMwareSdnApiFactory.createServiceApi(this.vs);

        try {
            serviceApi.deleteService(sId);
        } catch (Exception e) {
            ServiceElement service = serviceApi.findService(this.vs.getDistributedAppliance().getName());
            if (service != null) {
                throw e;
            }
        }

        LOG.debug("Updating nsx svc " + sId + " for VirtualSystem: " + this.vs.getId());
        this.vs = em.find(VirtualSystem.class, this.vs.getId());
        this.vs.setNsxServiceId(null);
        OSCEntityManager.update(em, this.vs);
    }

    @Override
    public String getName() {
        return "Delete Service '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
