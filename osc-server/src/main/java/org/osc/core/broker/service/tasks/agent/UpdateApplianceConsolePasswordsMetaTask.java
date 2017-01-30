package org.osc.core.broker.service.tasks.agent;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;

public class UpdateApplianceConsolePasswordsMetaTask extends TransactionalMetaTask {

    private static final Logger log = Logger.getLogger(UpdateApplianceConsolePasswordsMetaTask.class);

    private TaskGraph tg;
    private String newPassword;
    private List<DistributedApplianceInstance> daiList;

    public UpdateApplianceConsolePasswordsMetaTask(String newPassword, List<DistributedApplianceInstance> daiList) {
        this.newPassword = newPassword;
        this.daiList = daiList;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        log.info("Start executing appliance(s) console password update meta task");

        tg = new TaskGraph();
        for (DistributedApplianceInstance dai : daiList) {
            // We compare current password with the one new one to be set. If they are the same
            // There is no need to update the password
            if (this.newPassword != null && dai.getCurrentConsolePassword() != null
                    && dai.getCurrentConsolePassword().equals(this.newPassword)) {
                log.info("DAI '" + dai.getName() + "' console password is already up to date and thus skipped");
                continue;
            }

            // We indicate new password is pending to be applied. It will be set to null
            // once successfully applied.
            dai.setNewConsolePassword(this.newPassword);
            EntityManager.update(session, dai);

            tg.addTask(new UpdateApplianceConsolePasswordTask(dai, newPassword));
        }
    }

    @Override
    public String getName() {
        return "Propagating console password to DAIs";
    }

    @Override
    public TaskGraph getTaskGraph() {
        return tg;
    }
}
