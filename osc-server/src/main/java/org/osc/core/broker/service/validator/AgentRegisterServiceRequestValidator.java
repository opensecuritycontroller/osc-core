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
package org.osc.core.broker.service.validator;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.AgentRegisterServiceRequest;
import org.osc.core.broker.util.TransactionalBroadcastUtil;

public class AgentRegisterServiceRequestValidator implements RequestValidator<AgentRegisterServiceRequest, DistributedApplianceInstance> {
    private EntityManager em;
    private static final Logger log = Logger.getLogger(AgentRegisterServiceRequestValidator.class);
    private TransactionalBroadcastUtil txBroadcastUtil;

    public AgentRegisterServiceRequestValidator(EntityManager em, TransactionalBroadcastUtil txBroadcastUtil) {
        this.em = em;
        this.txBroadcastUtil = txBroadcastUtil;
    }

    @Override
    public void validate(AgentRegisterServiceRequest request) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public DistributedApplianceInstance validateAndLoad(AgentRegisterServiceRequest request) throws Exception {
        OSCEntityManager<DistributedApplianceInstance> emgr = new OSCEntityManager<DistributedApplianceInstance>(
                DistributedApplianceInstance.class, this.em, this.txBroadcastUtil);

        DistributedApplianceInstance dai = null;

        if (request.getApplianceIp() == null) {
            throw new VmidcBrokerValidationException("Missing agent IP address.");
        }

        if (request.getVsId() == null) {
            throw new VmidcBrokerValidationException("Invalid virtual system identifier.");
        }

        // retrieve existing entry from db search by name
        if (request.getName() != null) {
            dai = emgr.findByFieldName("name", request.getName());
            if (dai == null) {
                log.warn("DAI '" + request.getName() + "' is registered but cannot be found in the database.");
            }
        }

        // if not found by name, search by ip
        if (dai == null && request.getApplianceIp() != null) {
            dai = emgr.findByFieldName("ipAddress", request.getApplianceIp());
            if (dai != null) {
                log.warn("DAI found by IP '" + request.getApplianceIp() + "'");
            }
        }

        VirtualSystem vs = null;
        if (dai != null) {
            vs = dai.getVirtualSystem();
        } else {
            OSCEntityManager<VirtualSystem> vsMgr = new OSCEntityManager<VirtualSystem>(VirtualSystem.class, this.em, this.txBroadcastUtil);
            vs = vsMgr.findByPrimaryKey(request.getVsId());
        }

        if (vs == null) {
            throw new VmidcBrokerValidationException("VS ID " + request.getVsId() + " not found.");
        }

        return dai;
    }
}
