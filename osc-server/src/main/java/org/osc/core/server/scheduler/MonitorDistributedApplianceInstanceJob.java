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
package org.osc.core.server.scheduler;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockObjectReference.ObjectType;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.events.DaiFailureType;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.util.StaticRegistry;
import org.osc.core.broker.util.db.HibernateUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class MonitorDistributedApplianceInstanceJob implements Job {

    private static final long AGENT_UPDATE_THRESHOLD = 240000; //4 minutes

    private static final Logger log = Logger.getLogger(MonitorDistributedApplianceInstanceJob.class);

    public MonitorDistributedApplianceInstanceJob() {

    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        try {
            EntityManager em = HibernateUtil.getTransactionalEntityManager();
            HibernateUtil.getTransactionControl().required(() -> {
                OSCEntityManager<DistributedApplianceInstance> emgr = new OSCEntityManager<DistributedApplianceInstance>(
                        DistributedApplianceInstance.class, em, StaticRegistry.transactionalBroadcastUtil());
                List<DistributedApplianceInstance> dais = emgr.listAll();

                for (DistributedApplianceInstance dai : dais) {
                    Date date = null;
                    if (dai.getLastStatus() != null) {
                        date = new Date(dai.getLastStatus().getTime() + AGENT_UPDATE_THRESHOLD);
                    }
                    // Generate an alert if it has been more than 4 minutes since we last heard from the DAI
                    if (date == null || new Date().compareTo(date) > 0) {
                        log.warn("Generate an alert for DAI '" + dai.getName()
                        + "' since we have not receive expected registration request (every 3 minutes)");
                        StaticRegistry.alertGenerator().processDaiFailureEvent(DaiFailureType.DAI_TIMEOUT,
                                new LockObjectReference(dai.getId(), dai.getName(),
                                        ObjectType.DISTRIBUTED_APPLIANCE_INSTANCE),
                                "Health status information for Appliance Instance '" + dai.getName()
                                + "' not timely reported and is out of date");

                        dai.setDiscovered(null);
                        dai.setInspectionReady(null);
                    }
                }
                return null;
            });

        } catch (Exception ex) {
            log.error("Exception iterating over DAIs", ex);

        }
    }
}
