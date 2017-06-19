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

import org.apache.log4j.Logger;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.vc.DeleteVirtualizationConnectorServiceApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.virtualizationconnector.VCDeleteMetaTask;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class DeleteVirtualizationConnectorService extends ServiceDispatcher<BaseIdRequest, BaseJobResponse>
    implements DeleteVirtualizationConnectorServiceApi {

    private static final Logger log = Logger.getLogger(DeleteVirtualizationConnectorService.class);

    @Reference
    VCDeleteMetaTask deleteMetaTask;

    @Override
    public BaseJobResponse exec(BaseIdRequest request, EntityManager em) throws Exception {
        validate(em, request);

        OSCEntityManager<VirtualizationConnector> vcEntityMgr = new OSCEntityManager<>(VirtualizationConnector.class, em, this.txBroadcastUtil);
        VirtualizationConnector vc = vcEntityMgr.findByPrimaryKey(request.getId());
        return new BaseJobResponse(startJob(vc));
    }

    void validate(EntityManager em, BaseIdRequest request) throws Exception {
        VirtualizationConnector vc = em.find(VirtualizationConnector.class, request.getId());

        // entry must pre-exist in db
        if (vc == null) { // note: we cannot use name here in error msg since del req does not have name, only ID
            throw new VmidcBrokerValidationException("Virtualization Connector entry with ID " + request.getId() + " is not found.");
        }

        VirtualizationConnectorEntityMgr.validateCanBeDeleted(em, vc);
    }

    private Long startJob(VirtualizationConnector vc) throws Exception {

        log.info("Start VC (id:" + vc.getId() + ") delete Job");

        TaskGraph tg = new TaskGraph();

        tg.addTask(this.deleteMetaTask.create(vc));
        Job job = JobEngine.getEngine().submit("Delete Virtualization Connector '" + vc.getName() + "'", tg,
                LockObjectReference.getObjectReferences(vc));
        return job.getId();
    }

}
