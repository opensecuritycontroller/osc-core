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
package org.osc.core.broker.service.vc;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.VirtualizationConnectorConformJobFactory;
import org.osc.core.broker.service.api.SyncVirtualizationConnectorServiceApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseJobRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class SyncVirtualizationConnectorService extends ServiceDispatcher<BaseJobRequest, BaseJobResponse>
        implements SyncVirtualizationConnectorServiceApi {
    @Reference
    private VirtualizationConnectorConformJobFactory vcConformJobFactory;

    @Override
    public BaseJobResponse exec(BaseJobRequest request, EntityManager em) throws Exception {
        request.validateId();
        OSCEntityManager<VirtualizationConnector> emgr = new OSCEntityManager<>(VirtualizationConnector.class, em, this.txBroadcastUtil);
        VirtualizationConnector vc = emgr.findByPrimaryKey(request.getId());
        validate(request, vc);
        Long jobId = this.vcConformJobFactory.startVCSyncJob(vc, em).getId();
        return new BaseJobResponse(vc.getId(), jobId);
    }

    private void validate(BaseJobRequest request, VirtualizationConnector vc) throws Exception {
        // check for uniqueness of vc IP
        if (vc == null) {
            throw new VmidcBrokerValidationException("Virtualization Connector Id " + request.getId() + " not found.");
        }
    }
}