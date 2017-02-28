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

import org.hibernate.Session;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.SyncApplianceManagerConnectorRequest;
import org.osc.core.broker.service.response.SyncApplianceManagerConnectorResponse;

public class SyncManagerConnectorService extends
        ServiceDispatcher<SyncApplianceManagerConnectorRequest, SyncApplianceManagerConnectorResponse> {

    @Override
    public SyncApplianceManagerConnectorResponse exec(SyncApplianceManagerConnectorRequest request, Session session)
            throws Exception {

        EntityManager<ApplianceManagerConnector> emgr = new EntityManager<ApplianceManagerConnector>(
                ApplianceManagerConnector.class, session);

        ApplianceManagerConnector mc = emgr.findByPrimaryKey(request.getId());

        validate(request, mc);

        Long jobId = ConformService.startMCConformJob(mc, session).getId();

        SyncApplianceManagerConnectorResponse response = new SyncApplianceManagerConnectorResponse();
        response.setJobId(jobId);

        return response;
    }

    void validate(SyncApplianceManagerConnectorRequest request, ApplianceManagerConnector mc) throws Exception {

        // check for uniqueness of mc IP
        if (mc == null) {

            throw new VmidcBrokerValidationException("Appliance Manager Connector Id " + request.getId()
                    + " not found.");
        }

    }
}
