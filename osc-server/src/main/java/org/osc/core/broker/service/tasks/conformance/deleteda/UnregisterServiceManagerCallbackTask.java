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

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.rest.client.nsx.model.ServiceManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.sdn.api.ServiceManagerApi;
import org.osc.sdk.sdn.element.ServiceManagerElement;

public class UnregisterServiceManagerCallbackTask extends TransactionalTask {
    private VirtualSystem vs;

    public UnregisterServiceManagerCallbackTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        ServiceManagerApi serviceManagerApi = VMwareSdnApiFactory.createServiceManagerApi(this.vs);
        ServiceManagerElement serviceManagerElement = serviceManagerApi.getServiceManager(this.vs.getNsxServiceManagerId());
        ServiceManager serviceManager = new ServiceManager(serviceManagerElement);
        serviceManager.setRestUrl(null);
        serviceManager.setLogin(null);
        serviceManager.setPassword(null);
        serviceManager.setVerifyPassword(null);

        serviceManagerApi.updateServiceManager(serviceManager);
    }

    @Override
    public String getName() {
        return "Remove callbacks registration from Service Manager '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
