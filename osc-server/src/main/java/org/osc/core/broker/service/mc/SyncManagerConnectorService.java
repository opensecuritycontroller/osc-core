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
package org.osc.core.broker.service.mc;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.service.ManagerConnectorConformJobFactory;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.SyncManagerConnectorServiceApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseJobRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class SyncManagerConnectorService extends ServiceDispatcher<BaseJobRequest, BaseJobResponse>
        implements SyncManagerConnectorServiceApi {

    @Reference
    private ManagerConnectorConformJobFactory mcConformJobFactory;

    @Override
    public BaseJobResponse exec(BaseJobRequest request, EntityManager em) throws Exception {
        request.validateId();
        OSCEntityManager<ApplianceManagerConnector> emgr = new OSCEntityManager<>(ApplianceManagerConnector.class, em, this.txBroadcastUtil);
        ApplianceManagerConnector mc = emgr.findByPrimaryKey(request.getId());
        validate(request, mc);
        Long jobId = this.mcConformJobFactory.startMCConformJob(mc, em).getId();
        return new BaseJobResponse(mc.getId(), jobId);
    }

    private void validate(BaseJobRequest request, ApplianceManagerConnector mc) throws Exception {
        // check for uniqueness of mc IP
        if (mc == null) {
            throw new VmidcBrokerValidationException("Appliance Manager Connector Id " + request.getId() + " not found.");
        }
    }
}
