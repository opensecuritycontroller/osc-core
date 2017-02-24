/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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
