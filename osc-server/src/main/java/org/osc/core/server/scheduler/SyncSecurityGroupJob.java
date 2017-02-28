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

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.rest.server.VmidcAuthFilter;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.util.SessionUtil;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.broker.util.db.TransactionalRunner;
import org.osc.core.broker.util.db.TransactionalRunner.TransactionalAction;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class SyncSecurityGroupJob implements Job {

    private static final Logger log = Logger.getLogger(SyncSecurityGroupJob.class);

    public SyncSecurityGroupJob() {

    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        SessionUtil.setUser(VmidcAuthFilter.VMIDC_DEFAULT_LOGIN);
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            EntityManager<SecurityGroup> emgr = new EntityManager<SecurityGroup>(SecurityGroup.class, session);
            Transaction tx = session.beginTransaction();
            List<SecurityGroup> sgs = emgr.listAll();
            tx.commit();

            for (final SecurityGroup sg : sgs) {
                if (sg.getVirtualizationConnector().isVmware()) {
                    continue;
                }
                Thread sgSync = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        new TransactionalRunner<Object, SecurityGroup>(new TransactionalRunner.ExclusiveSessionHandler())
                            .exec(new TransactionalAction<Object, SecurityGroup>() {

                            @Override
                            public Object run(Session session, SecurityGroup sg) {

                                SessionUtil.setUser(VmidcAuthFilter.VMIDC_DEFAULT_LOGIN);

                                try {
                                    sg = (SecurityGroup) session.get(SecurityGroup.class, sg.getId());
                                    ConformService.startSecurityGroupConformanceJob(sg);
                                } catch (Exception ex) {
                                    AlertGenerator.processSystemFailureEvent(SystemFailureType.SCHEDULER_FAILURE,
                                            new LockObjectReference(sg),
                                            "Failure during scheduling of Security Group Sync. " + ex.getMessage());
                                    log.error("Fail to sync SG " + sg.getName(), ex);
                                }
                                return null;
                            }
                        }, sg);

                    }
                }, "Scheduled-SG-Sync-runner-Thread-" + System.currentTimeMillis());

                sgSync.start();
            }
        } catch (Exception ex) {
            AlertGenerator.processSystemFailureEvent(SystemFailureType.SCHEDULER_FAILURE, null,
                    "Failure during scheduling of Security Groups Sync. " + ex.getMessage());
            log.error("Fail to get database session or query SGs", ex);

        } finally {

            if (session != null) {
                session.close();
            }
        }
    }

}
