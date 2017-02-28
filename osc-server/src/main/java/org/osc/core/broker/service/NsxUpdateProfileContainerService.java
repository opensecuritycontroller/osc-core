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

import org.hibernate.Session;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.SecurityGroupInterfaceEntityMgr;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.request.NsxUpdateProfileContainerRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.securitygroup.MgrSecurityGroupCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroup.NsxServiceProfileContainerCheckMetaTask;
import org.osc.core.util.NetworkUtil;
import org.osc.sdk.manager.api.ApplianceManagerApi;

public class NsxUpdateProfileContainerService extends
ServiceDispatcher<NsxUpdateProfileContainerRequest, BaseJobResponse> {

    //private static final Logger log = Logger.getLogger(NsxUpdateProfileContainerService.class);

    @Override
    public BaseJobResponse exec(NsxUpdateProfileContainerRequest request, Session session) throws Exception {

        TaskGraph tg = new TaskGraph();

        VirtualSystem vs = VirtualSystemEntityMgr.findByNsxServiceProfileIdAndNsxIp(session, request.serviceProfileId,
                request.nsxIpAddress);

        if (vs == null) {
            String host = NetworkUtil.resolveIpToName(request.nsxIpAddress);
            if (host != null) {
                vs = VirtualSystemEntityMgr.findByNsxServiceProfileIdAndNsxIp(session, request.serviceProfileId, host);
            }
            // If we cannot find the VS, that means this is an invalid call.
            if (vs == null) {
                throw new VmidcBrokerValidationException("VS For NSX server IP: " + request.nsxIpAddress
                        + " and Service Profile Id: " + request.serviceProfileId + " not found.");
            }
        }

        SecurityGroupInterface sgi = SecurityGroupInterfaceEntityMgr.findSecurityGroupInterfaceByVsAndTag(session, vs,
                request.serviceProfileId);

        NsxServiceProfileContainerCheckMetaTask syncTask = new NsxServiceProfileContainerCheckMetaTask(sgi,
                request.containerSet);
        tg.addTask(syncTask);

        ApplianceManagerApi managerApi = ManagerApiFactory.createApplianceManagerApi(vs);
        if (vs.getMgrId() != null && managerApi.isSecurityGroupSyncSupport()) {
            tg.addTask(new MgrSecurityGroupCheckMetaTask(vs), syncTask);
        }

        Job job = JobEngine.getEngine().submit(
                "Syncing NSX Service Profile '" + sgi.getName() + "' (" + sgi.getTag() + ") Container", tg,
                LockObjectReference.getObjectReferences(vs));

        BaseJobResponse response = new BaseJobResponse();
        response.setId(job.getId());

        return response;
    }

}
