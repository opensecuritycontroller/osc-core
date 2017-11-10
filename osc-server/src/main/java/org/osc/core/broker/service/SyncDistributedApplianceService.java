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

import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.service.api.SyncDistributedApplianceServiceApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.util.ValidateUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class SyncDistributedApplianceService extends ServiceDispatcher<BaseIdRequest, BaseJobResponse>
    implements SyncDistributedApplianceServiceApi {

    private DistributedAppliance da;

    @Reference
    private DistributedApplianceConformJobFactory daConformJobFactory;

    @Override
    protected BaseJobResponse exec(BaseIdRequest request, EntityManager em) throws Exception {
        this.da = em.find(DistributedAppliance.class, request.getId());

        validate(em, request.getId());

        Long jobId = this.daConformJobFactory.startDAConformJob(em, this.da);

        BaseJobResponse response = new BaseJobResponse();
        response.setJobId(jobId);

        return response;
    }

    protected void validate(EntityManager em, long daId) throws Exception {
        if (this.da == null) {
            throw new VmidcBrokerValidationException("Distributed Appliance with Id: " + daId
            + "  is not found.");
        }

        ValidateUtil.checkMarkedForDeletion(this.da, this.da.getName());
    }
}
