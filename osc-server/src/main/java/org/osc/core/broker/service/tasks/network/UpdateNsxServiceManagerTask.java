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
package org.osc.core.broker.service.tasks.network;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.rest.client.nsx.model.ServiceManager;
import org.osc.core.broker.rest.server.NsxAuthFilter;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.service.tasks.conformance.virtualsystem.CreateNsxServiceManagerTask;
import org.osc.sdk.sdn.api.ServiceManagerApi;
import org.osc.sdk.sdn.element.ServiceManagerElement;

public class UpdateNsxServiceManagerTask extends TransactionalTask {

    final Logger log = Logger.getLogger(UpdateNsxServiceManagerTask.class);

    private VirtualSystem vs;

    public UpdateNsxServiceManagerTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());
        ServiceManagerApi serviceManagerApi = VMwareSdnApiFactory.createServiceManagerApi(this.vs);
        ServiceManagerElement serviceManagerElement = serviceManagerApi.getServiceManager(this.vs.getNsxServiceManagerId());
        ServiceManager serviceManager = new ServiceManager(serviceManagerElement);

        serviceManager.setRestUrl(CreateNsxServiceManagerTask.buildRestCallbackUrl());
        serviceManager.setPassword(NsxAuthFilter.VMIDC_NSX_PASS);
        serviceManager.setVerifyPassword(NsxAuthFilter.VMIDC_NSX_PASS);

        serviceManager.setVendorName(CreateNsxServiceManagerTask.generateServiceManagerName(this.vs));
        serviceManager.setVendorId(CreateNsxServiceManagerTask.generateServiceManagerName(this.vs));

        serviceManager.setName(CreateNsxServiceManagerTask.generateServiceManagerName(this.vs));
        serviceManager.setDescription(ServiceManager.VENDOR_DESCRIPTION);
        serviceManagerApi.updateServiceManager(serviceManager);
    }

    @Override
    public String getName() {
        return "Updating attributes for NSX Service Manager '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
