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
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.sdn.api.ServiceInstanceApi;

public class RegisterServiceInstanceTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(RegisterServiceInstanceTask.class);

    private VirtualSystem vs;

    public RegisterServiceInstanceTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        LOG.debug("Start executing GetServiceInstanceTask");
        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());

        ServiceInstanceApi serviceInstanceApi = VMwareSdnApiFactory.createServiceInstanceApi(this.vs);
        String serviceInstanceId = serviceInstanceApi.createServiceInstance(this.vs.getDistributedAppliance().getName() + "-GlobalInstance", this.vs.getNsxServiceId());

        // persist the svcInstanceId
        this.vs.setNsxServiceInstanceId(serviceInstanceId);
        EntityManager.update(session, this.vs);
    }

    @Override
    public String getName() {
        return "Register Service Instance '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }
}
