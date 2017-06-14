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
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.service.api.DeleteApplianceManagerConnectorServiceApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DistributedApplianceEntityMgr;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.manager.MCDeleteMetaTask;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class DeleteApplianceManagerConnectorService extends
        ServiceDispatcher<BaseIdRequest, BaseJobResponse> implements DeleteApplianceManagerConnectorServiceApi {

    private static final Logger log = Logger.getLogger(DeleteApplianceManagerConnectorService.class);

    @Reference
    MCDeleteMetaTask mcDeleteMetaTask;

    @Override
    public BaseJobResponse exec(BaseIdRequest request, EntityManager em) throws Exception {
        ApplianceManagerConnector mc = em.find(ApplianceManagerConnector.class, request.getId());
        validate(em, request, mc);
        return new BaseJobResponse(startJob(mc));
    }

    private void validate(EntityManager em, BaseIdRequest request, ApplianceManagerConnector mc)
            throws Exception {

        if (mc == null) {

            throw new VmidcBrokerValidationException("Appliance Manager Connector with ID " + request.getId()
                    + " is not found.");
        }

        if (DistributedApplianceEntityMgr.isReferencedByDistributedAppliance(em, mc)) {

            throw new VmidcBrokerInvalidRequestException(
                    "Cannot delete Appliance Manager Connector that is referenced by a Distributed Appliance.");
        }
    }

    private Long startJob(ApplianceManagerConnector mc) throws Exception {

        log.info("Start MC (id:" + mc.getId() + ") delete Job");

        TaskGraph tg = new TaskGraph();

        tg.addTask(this.mcDeleteMetaTask.create(mc));
        Job job = JobEngine.getEngine().submit("Delete Appliance Manager Connector '" + mc.getName() + "'", tg,
                LockObjectReference.getObjectReferences(mc));
        return job.getId();
    }
}
