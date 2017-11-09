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
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.service.SecurityGroupConformJobFactory;
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

public class SyncSecurityGroupJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(SyncSecurityGroupJob.class);

    public SyncSecurityGroupJob() {

    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        SessionUtil.getInstance().setUser(RestConstants.OSC_DEFAULT_LOGIN);
        SecurityGroupConformJobFactory sgConformJobFactory =
                (SecurityGroupConformJobFactory) context.getMergedJobDataMap().get(SecurityGroupConformJobFactory.class.getName());
        try {
            EntityManager em = HibernateUtil.getTransactionalEntityManager();
            List<SecurityGroup> sgs = HibernateUtil.getTransactionControl().required(() -> {
                OSCEntityManager<SecurityGroup> emgr = new OSCEntityManager<SecurityGroup>(SecurityGroup.class, em, StaticRegistry.transactionalBroadcastUtil());
                return emgr.listAll();
            });

            for (final SecurityGroup sg : sgs) {
                // TODO emanoel: remove this condition once SG sync is implemented.
                if (sg.getVirtualizationConnector().getVirtualizationType().isKubernetes()) {
                    continue;
                }

                Thread sgSync = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            HibernateUtil.getTransactionControl().required(() -> {
                                SessionUtil.getInstance().setUser(RestConstants.OSC_DEFAULT_LOGIN);
                                EntityManager em = HibernateUtil.getTransactionalEntityManager();
                                try {
                                    SecurityGroup found = em.find(SecurityGroup.class, sg.getId());
                                    sgConformJobFactory.startSecurityGroupConformanceJob(found);
                                } catch (Exception ex) {
                                    StaticRegistry.alertGenerator().processSystemFailureEvent(SystemFailureType.SCHEDULER_FAILURE,
                                            new LockObjectReference(sg),
                                            "Failure during scheduling of Security Group Sync. " + ex.getMessage());
                                    log.error("Fail to sync SG " + sg.getName(), ex);
                                }
                                return null;

                            });
                        } catch (Exception e) {
                            // Just let the thread finish
                        }
                    }
                }, "Scheduled-SG-Sync-runner-Thread-" + System.currentTimeMillis());

                sgSync.start();
            }
        } catch (ScopedWorkException ex) {
            StaticRegistry.alertGenerator().processSystemFailureEvent(SystemFailureType.SCHEDULER_FAILURE, null,
                    "Failure during scheduling of Security Groups Sync. " + ex.getCause().getMessage());
            log.error("Fail to get database session or query SGs", ex.getCause());
        } catch (Exception ex) {
            StaticRegistry.alertGenerator().processSystemFailureEvent(SystemFailureType.SCHEDULER_FAILURE, null,
                    "Failure during scheduling of Security Groups Sync. " + ex.getMessage());
            log.error("Fail to get database session or query SGs", ex);
        }
    }

}
