package org.osc.core.broker.service.tasks.mgrfile;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;



public class MgrFileChangePropagateTask extends TransactionalMetaTask {

    private static final Logger log = Logger.getLogger(MgrFileChangePropagateTask.class);

    private TaskGraph tg;
    private byte[] mgrFile = null;
    private String mgrFileName;
    private List<DistributedApplianceInstance> daiList;

    public MgrFileChangePropagateTask(String mgrFileName, byte[] mgrFile, List<DistributedApplianceInstance> daiList) {
        this.mgrFileName = mgrFileName;
        this.mgrFile = mgrFile;
        this.daiList = daiList;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        log.debug("Start executing mgrfile Propagating task");

        tg = new TaskGraph();

        for (DistributedApplianceInstance dai : daiList) {
            tg.addTask(new MgrFileChangePropagateToDaiTask(dai, mgrFile, mgrFileName));
        }

    }

    @Override
    public String getName() {
        return "Propagating Manager File '" + mgrFileName + "' to DAIs";
    }

    @Override
    public TaskGraph getTaskGraph() {
        return tg;
    }

}
