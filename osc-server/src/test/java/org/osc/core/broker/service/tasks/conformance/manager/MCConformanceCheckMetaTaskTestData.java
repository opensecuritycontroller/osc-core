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
package org.osc.core.broker.service.tasks.conformance.manager;

import java.util.ArrayList;
import java.util.List;

import org.osc.core.broker.job.Task;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.common.job.TaskGuard;

class MCConformanceCheckMetaTaskTestData {
    public static List<ApplianceManagerConnector> TEST_MANAGER_CONNECTORS = new ArrayList<ApplianceManagerConnector>();

    public static ApplianceManagerConnector POLICY_MAPPING_SUPPORTED_MC = createManagerConnector(1L, "NSM");
    public static ApplianceManagerConnector POLICY_MAPPING_NOT_SUPPORTED_MC = createManagerConnector(2L, "SMC");
    public static byte[] PUBLIC_KEY = new byte[3];

    public static TaskGraph createMcPolicyMappingNotSupportedGraph(ApplianceManagerConnector mc) throws Exception {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTaskGraph(createSyncPublicKeyGraph(mc));
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(mc), LockType.WRITE_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        return expectedGraph;
    }

    public static TaskGraph createMcPolicyMappingSupportedGraph(ApplianceManagerConnector mc) throws Exception {
        TaskGraph expectedGraph = new TaskGraph();
        expectedGraph.addTaskGraph(createSyncPublicKeyGraph(mc));

        Task syncDomains = new SyncDomainMetaTask().create(mc);
        expectedGraph.addTask(syncDomains);
        expectedGraph.addTask(new SyncPolicyMetaTask().create(mc), syncDomains);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(mc), LockType.WRITE_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        return expectedGraph;
    }

    private static TaskGraph createSyncPublicKeyGraph(ApplianceManagerConnector mc) throws Exception {
        TaskGraph tg = new TaskGraph();
        tg.addTask(new SyncMgrPublicKeyTask().create(mc, PUBLIC_KEY));
        return tg;
    }

    private static ApplianceManagerConnector createManagerConnector(Long mcId, String managerType) {
        ApplianceManagerConnector mc = new ApplianceManagerConnector();
        mc.setId(mcId);
        mc.setManagerType(managerType);
        TEST_MANAGER_CONNECTORS.add(mc);
        return mc;
    }
}
