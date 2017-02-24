package org.osc.core.broker.service.tasks.conformance.manager;

import java.util.ArrayList;
import java.util.List;

import org.osc.core.broker.job.Task;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.service.tasks.conformance.manager.SyncDomainMetaTask;
import org.osc.core.broker.service.tasks.conformance.manager.SyncMgrPublicKeyTask;
import org.osc.core.broker.service.tasks.conformance.manager.SyncPolicyMetaTask;

class MCConformanceCheckMetaTaskTestData {
    public static List<ApplianceManagerConnector> TEST_MANAGER_CONNECTORS = new ArrayList<ApplianceManagerConnector>();

    public static ApplianceManagerConnector POLICY_MAPPING_SUPPORTED_MC = createManagerConnector(1L, ManagerType.NSM);
    public static ApplianceManagerConnector POLICY_MAPPING_NOT_SUPPORTED_MC = createManagerConnector(2L, ManagerType.SMC);
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

        Task syncDomains = new SyncDomainMetaTask(mc);
        expectedGraph.addTask(syncDomains);
        expectedGraph.addTask(new SyncPolicyMetaTask(mc), syncDomains);
        expectedGraph.appendTask(new UnlockObjectTask(new LockObjectReference(mc), LockType.WRITE_LOCK), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        return expectedGraph;
    }

    private static TaskGraph createSyncPublicKeyGraph(ApplianceManagerConnector mc) throws Exception {
        TaskGraph tg = new TaskGraph();
        tg.addTask(new SyncMgrPublicKeyTask(mc, PUBLIC_KEY));
        return tg;
    }

    private static ApplianceManagerConnector createManagerConnector(Long mcId, ManagerType managerType) {
        ApplianceManagerConnector mc = new ApplianceManagerConnector();
        mc.setId(mcId);
        mc.setManagerType(managerType);
        TEST_MANAGER_CONNECTORS.add(mc);
        return mc;
    }
}
