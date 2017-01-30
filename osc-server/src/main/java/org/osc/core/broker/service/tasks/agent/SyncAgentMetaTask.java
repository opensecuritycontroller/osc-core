package org.osc.core.broker.service.tasks.agent;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.network.IpChangePropagateToDaiTask;
import org.osc.core.broker.service.tasks.passwordchange.PasswordChangePropagateToDaiTask;
import org.osc.sdk.manager.api.ApplianceManagerApi;

import com.mcafee.vmidc.server.Server;

public class SyncAgentMetaTask extends TransactionalMetaTask {

    private static final Logger log = Logger.getLogger(SyncAgentMetaTask.class);
    private TaskGraph tg;
    private List<DistributedApplianceInstance> daiList;

    public SyncAgentMetaTask(List<DistributedApplianceInstance> daiList) {
        this.daiList = daiList;
    }

    @Override
    public String getName() {
        return "Syncing " + Server.PRODUCT_NAME + " Agent(s)";
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        log.debug("Start Agent Sync meta task");
        this.tg = new TaskGraph();
        for (DistributedApplianceInstance dai : this.daiList) {
            TaskGraph agentTaskGraph = new TaskGraph();
            agentTaskGraph.appendTask(new PasswordChangePropagateToDaiTask(dai), TaskGuard.ALL_PREDECESSORS_COMPLETED);
            agentTaskGraph.appendTask(new IpChangePropagateToDaiTask(dai), TaskGuard.ALL_PREDECESSORS_COMPLETED);

            ApplianceManagerApi managerApi = ManagerApiFactory.createApplianceManagerApi(dai);
            if (managerApi.isSecurityGroupSyncSupport() && managerApi.isAgentManaged()) {
                agentTaskGraph.appendTask(new AgentInterfaceEndpointMapSetTask(dai),
                        TaskGuard.ALL_PREDECESSORS_COMPLETED);
            }

            this.tg.addTaskGraph(agentTaskGraph);
        }

    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

}
