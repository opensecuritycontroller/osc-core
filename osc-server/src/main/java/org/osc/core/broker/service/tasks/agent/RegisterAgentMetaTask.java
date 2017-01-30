package org.osc.core.broker.service.tasks.agent;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;

import com.mcafee.vmidc.server.Server;

public class RegisterAgentMetaTask extends TransactionalMetaTask {

    private static final Logger log = Logger.getLogger(RegisterAgentMetaTask.class);
    private TaskGraph tg;
    private List<DistributedApplianceInstance> daiList;

    public RegisterAgentMetaTask(List<DistributedApplianceInstance> daiList) {
        this.daiList = daiList;
    }

    @Override
    public String getName() {
        return "Register " + Server.PRODUCT_NAME + " Agent(s)";
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        log.debug("Start Agent Registeration task");
        this.tg = new TaskGraph();
        for (DistributedApplianceInstance dai : this.daiList) {
            // Reset initial config for SMC so a new one will be generated.
            dai.setApplianceConfig(null);
            EntityManager.update(session, dai);

            this.tg.addTask(new RegisterAgentTask(dai));
        }

    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

}
