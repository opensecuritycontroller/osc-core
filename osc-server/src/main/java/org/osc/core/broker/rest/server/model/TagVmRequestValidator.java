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
package org.osc.core.broker.rest.server.model;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.RequestValidator;

public class TagVmRequestValidator implements RequestValidator<TagVmRequest, DistributedApplianceInstance> {

    private EntityManager em;

    public TagVmRequestValidator(EntityManager em) {
        this.em = em;
    }

    @Override
    public void validate(TagVmRequest request) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public DistributedApplianceInstance validateAndLoad(TagVmRequest request) throws Exception {
        if (request == null || request.getApplianceInstanceName() == null || request.getApplianceInstanceName().isEmpty()) {
            throw new VmidcBrokerValidationException("Null request or invalid Appliance Instance Name.");
        }

        OSCEntityManager<DistributedApplianceInstance> emgr = new OSCEntityManager<>(DistributedApplianceInstance.class, this.em);
        DistributedApplianceInstance dai = emgr.findByFieldName("name", request.getApplianceInstanceName());

        if (dai == null) {
            throw new VmidcBrokerValidationException("Appliance Instance Name '" + request.getApplianceInstanceName() + "' not found.");
        }

        return dai;
    }
}
