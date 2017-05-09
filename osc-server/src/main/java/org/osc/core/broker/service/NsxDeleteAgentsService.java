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
package org.osc.core.broker.service;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.service.api.NsxDeleteAgentsServiceApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.NsxDeleteAgentsRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;
import org.osc.core.broker.service.tasks.conformance.manager.MgrDeleteMemberDeviceTask;
import org.osgi.service.component.annotations.Component;

@Component
public class NsxDeleteAgentsService extends ServiceDispatcher<NsxDeleteAgentsRequest, EmptySuccessResponse>
        implements NsxDeleteAgentsServiceApi {

    private static final Logger log = Logger.getLogger(NsxDeleteAgentsService.class);

    @Override
    public EmptySuccessResponse exec(NsxDeleteAgentsRequest request, EntityManager em) throws Exception {

        OSCEntityManager<DistributedApplianceInstance> emgr = new OSCEntityManager<DistributedApplianceInstance>(
                DistributedApplianceInstance.class, em);

        DistributedApplianceInstance dai = validate(em, request, emgr);

        if (dai != null) {
            if (MgrDeleteMemberDeviceTask.deleteMemberDevice(dai)) {
                OSCEntityManager.delete(em, dai);
            }
        } else {
            log.info("An unregistered nsx appliance agent '" + request.agentIds + "' had been undeployed.");
        }

        EmptySuccessResponse response = new EmptySuccessResponse();

        return response;
    }

    private DistributedApplianceInstance validate(EntityManager em, NsxDeleteAgentsRequest request,
            OSCEntityManager<DistributedApplianceInstance> emgr) throws Exception {

        DistributedApplianceInstance dai = null;

        if (request.nsxIpAddress == null) {
            throw new VmidcBrokerValidationException("Missing NSX IP Address.");
        }

        if (request.agentIds == null) {
            throw new VmidcBrokerValidationException("Missing Agent IDs.");
        }

        dai = DistributedApplianceInstanceEntityMgr.findByNsxAgentIdAndNsxIp(em, request.agentIds,
                request.nsxIpAddress);
        return dai;
    }

}
