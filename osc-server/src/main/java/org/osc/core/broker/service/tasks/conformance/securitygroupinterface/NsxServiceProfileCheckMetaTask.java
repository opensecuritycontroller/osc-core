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
package org.osc.core.broker.service.tasks.conformance.securitygroupinterface;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.rest.client.nsx.model.ServiceProfile;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;

public class NsxServiceProfileCheckMetaTask extends TransactionalMetaTask {
    private static final Logger log = Logger.getLogger(NsxServiceProfileCheckMetaTask.class);

    private VirtualSystem vs;
    private ServiceProfile serviceProfile;
    private TaskGraph tg;

    public NsxServiceProfileCheckMetaTask(VirtualSystem vs, ServiceProfile serviceProfile) {
        this.vs = vs;
        this.serviceProfile = serviceProfile;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        log.info("Start executing ServiceProfileCheckMetaTask task for VS '" + vs.getId() + "'");
        tg = new TaskGraph();

        vs = (VirtualSystem) session.get(VirtualSystem.class, vs.getId());

        NsxSecurityGroupInterfacesCheckMetaTask.processServiceProfile(session, tg, vs, serviceProfile);
    }

    @Override
    public String getName() {
        return "Checking Service Profile '" + serviceProfile.getName() + "' on Virtual System '"
                + vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public TaskGraph getTaskGraph() {
        return tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(vs);
    }

}
