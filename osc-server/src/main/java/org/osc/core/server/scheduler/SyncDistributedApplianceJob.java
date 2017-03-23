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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.rest.server.OscAuthFilter;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.util.SessionUtil;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.broker.util.db.TransactionalRunner;
import org.osc.core.broker.util.db.TransactionalRunner.TransactionalAction;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class SyncDistributedApplianceJob implements Job {

    private static final Logger log = Logger.getLogger(SyncDistributedApplianceJob.class);

    public SyncDistributedApplianceJob() {

    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        SessionUtil.setUser(OscAuthFilter.OSC_DEFAULT_LOGIN);
        ConformService conformService = (ConformService) context.get(ConformService.class.getName());
        EntityManager em = null;
        try {
            em = HibernateUtil.getEntityManagerFactory().createEntityManager();
            OSCEntityManager<DistributedAppliance> emgr = new OSCEntityManager<DistributedAppliance>(
                    DistributedAppliance.class, em);
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            List<DistributedAppliance> das = emgr.listAll();
            tx.commit();

            // Iterate on all DAs and execute a sync on a separate thread as we are placing lock and want to avoid
            // delays for following DAs
            for (final DistributedAppliance da : das) {
                Thread daSync = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        new TransactionalRunner<Object, DistributedAppliance>(new TransactionalRunner.ExclusiveSessionHandler())
                            .exec(new TransactionalAction<Object, DistributedAppliance>() {

                            @Override
                            public Object run(EntityManager em, DistributedAppliance da) {

                                        SessionUtil.setUser(OscAuthFilter.OSC_DEFAULT_LOGIN);

                                try {
                                    da = em.find(DistributedAppliance.class, da.getId());
                                    conformService.startDAConformJob(em, da, null, false);
                                } catch (Exception ex) {
                                    AlertGenerator.processSystemFailureEvent(
                                            SystemFailureType.SCHEDULER_FAILURE,
                                            new LockObjectReference(da),
                                            "Failure during scheduling of Distributed Appliance Sync. "
                                                    + ex.getMessage());
                                    log.error("Fail to sync DA " + da.getName(), ex);
                                }
                                return null;
                            }
                        }, da);

                    }
                }, "Scheduled-DA-Sync-runner-Thread-" + System.currentTimeMillis());

                daSync.start();

            }

        } catch (Exception ex) {
            AlertGenerator.processSystemFailureEvent(SystemFailureType.SCHEDULER_FAILURE, null,
                    "Failure during scheduling of Distributed Appliances Sync. " + ex.getMessage());
            log.error("Fail to get database session or query DAs", ex);

        } finally {

            if (em != null) {
                em.close();
            }
        }
    }
}
