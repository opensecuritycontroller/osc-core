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

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.JobQueuer;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.JobQueuer.JobRequest;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.rest.client.nsx.model.ServiceProfile;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.request.NsxUpdateProfileRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;
import org.osc.core.broker.service.tasks.conformance.LockObjectTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.service.tasks.conformance.securitygroup.MgrSecurityGroupCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroupinterface.MgrSecurityGroupInterfacesCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroupinterface.NsxServiceProfileCheckMetaTask;

public class NsxUpdateProfileService extends ServiceDispatcher<NsxUpdateProfileRequest, EmptySuccessResponse> {

    private static final Logger log = Logger.getLogger(NsxUpdateProfileService.class);

    @Override
    public EmptySuccessResponse exec(NsxUpdateProfileRequest request, Session session) throws Exception {

        startServiceProfileSyncJob(session, request.serviceProfile);

        return new EmptySuccessResponse();
    }

    private void startServiceProfileSyncJob(Session session, ServiceProfile serviceProfile) throws Exception {
        TaskGraph tg = new TaskGraph();

        VirtualSystem vs = VirtualSystemEntityMgr.findByNsxServiceInstanceIdAndVsmUuid(session,
                serviceProfile.getServiceInstance().vsmUuid, serviceProfile.getServiceInstance().objectId);

        // If we cant find the VS, that means this is a zombie VS call.
        if (vs == null) {
            throw new VmidcBrokerValidationException("VS For serviceVsmUuid: "
                    + serviceProfile.getServiceInstance().vsmUuid + " and serviceInstanceId: "
                    + serviceProfile.getServiceInstance().objectId + " not found.");
        }
        /*
         * If updates are done for deleted VS/DA, it is most like our job
         * triggering these changes. ignore them.
         */
        if (vs.getMarkedForDeletion() || vs.getDistributedAppliance().getMarkedForDeletion()) {
            return;
        }

        LockObjectReference or = new LockObjectReference(vs.getDistributedAppliance());
        UnlockObjectTask ult = new UnlockObjectTask(or, LockType.WRITE_LOCK);
        LockRequest lockRequest = new LockRequest(or, ult);
        tg.addTask(new LockObjectTask(lockRequest));

        // NSX->ISC sync of security group interfaces to reflect security group/service profile re-assignment
        tg.appendTask(new NsxServiceProfileCheckMetaTask(vs, serviceProfile));

        // If the appliance manager supports policy mapping then perform OSC->MC sync of security group interfaces
        if (vs.getMgrId() != null && ManagerApiFactory.createApplianceManagerApi(vs).isPolicyMappingSupported()) {
            tg.appendTask(new MgrSecurityGroupInterfacesCheckMetaTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        }

        // If the appliance manager supports security group sync then perform OSC->MC sync of security groups
        if (vs.getMgrId() != null && ManagerApiFactory.createApplianceManagerApi(vs).isSecurityGroupSyncSupport()) {
            tg.appendTask(new MgrSecurityGroupCheckMetaTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        }

        tg.appendTask(ult, TaskGuard.ALL_PREDECESSORS_COMPLETED);

        // There is an NSX bug which gives us multiple callbacks, we want to execute the callback responses
        // in order of receiving them so that we get accurate information(which is contained in the last callback)
        // so we use the queue mechanism.
        JobQueuer.getInstance().putJob(
                new JobRequest("Syncing Service Profile '" + serviceProfile.getName()
                + "' Policy bindings for Virtual System '" + vs.getVirtualizationConnector().getName() + "'",
                tg, LockObjectReference.getObjectReferences(vs)));
        log.debug("Done Adding Syncing Service Profile Job to job queue");
    }
}
