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

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.service.DistributedApplianceConformJobFactory;
import org.osc.core.broker.service.api.RestConstants;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.util.SessionUtil;
import org.osc.core.broker.util.StaticRegistry;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osgi.service.transaction.control.ScopedWorkException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncDistributedApplianceJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(SyncDistributedApplianceJob.class);

    public SyncDistributedApplianceJob() {

    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        SessionUtil.getInstance().setUser(RestConstants.OSC_DEFAULT_LOGIN);
        DistributedApplianceConformJobFactory daConformJobFactory = (DistributedApplianceConformJobFactory) context.getMergedJobDataMap().get(DistributedApplianceConformJobFactory.class.getName());
        try {
            EntityManager em = HibernateUtil.getTransactionalEntityManager();

            List<DistributedAppliance> das = HibernateUtil.getTransactionControl().required(() -> {
                OSCEntityManager<DistributedAppliance> emgr = new OSCEntityManager<DistributedAppliance>(
                        DistributedAppliance.class, em, StaticRegistry.transactionalBroadcastUtil());
                return emgr.listAll();
            });

            // Iterate on all DAs and execute a sync on a separate thread as we are placing lock and want to avoid
            // delays for following DAs
            for (final DistributedAppliance da : das) {
                Thread daSync = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            HibernateUtil.getTransactionControl().required(() -> {
                                SessionUtil.getInstance().setUser(RestConstants.OSC_DEFAULT_LOGIN);
                                EntityManager em = HibernateUtil.getTransactionalEntityManager();
                                try {
                                    DistributedAppliance found = em.find(DistributedAppliance.class, da.getId());
                                    daConformJobFactory.startDAConformJob(em, found, null, false);
                                } catch (Exception ex) {
                                    StaticRegistry.alertGenerator().processSystemFailureEvent(
                                            SystemFailureType.SCHEDULER_FAILURE,
                                            new LockObjectReference(da),
                                            "Failure during scheduling of Distributed Appliance Sync. "
                                                    + ex.getMessage());
                                    log.error("Fail to sync DA " + da.getName(), ex);
                                }
                                return null;
                            });
                        } catch (Exception e) {
                            // Just let the thread finish
                        }
                    }
                }, "Scheduled-DA-Sync-runner-Thread-" + System.currentTimeMillis());

                daSync.start();

            }

        } catch (ScopedWorkException ex) {
            StaticRegistry.alertGenerator().processSystemFailureEvent(SystemFailureType.SCHEDULER_FAILURE, null,
                    "Failure during scheduling of Distributed Appliances Sync. " + ex.getCause().getMessage());
            log.error("Fail to get database session or query DAs", ex.getCause());
        } catch (Exception ex) {
            StaticRegistry.alertGenerator().processSystemFailureEvent(SystemFailureType.SCHEDULER_FAILURE, null,
                    "Failure during scheduling of Distributed Appliances Sync. " + ex.getMessage());
            log.error("Fail to get database session or query DAs", ex);

        }
    }
}
