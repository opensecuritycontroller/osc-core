package org.osc.core.broker.service.tasks.agent;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;

public class UpgradeAgentMetaTask extends TransactionalMetaTask {

    private static final Logger log = Logger.getLogger(UpgradeAgentMetaTask.class);
    private TaskGraph tg;
    private List<DistributedApplianceInstance> daiList;

    public UpgradeAgentMetaTask(List<DistributedApplianceInstance> daiList) {
        this.daiList = daiList;
    }

    @Override
    public TaskGraph getTaskGraph() {
        return tg;
    }

    @Override
    public String getName() {
        return "Upgrade Security broker Agent(s)";
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        log.debug("Start Agent Upgrade task");
        tg = new TaskGraph();
        for (DistributedApplianceInstance dai : daiList) {
            tg.addTask(new UpgradeAgentTask(dai));
        }
    }

}
