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
package org.osc.core.broker.service.persistence;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.joda.time.DateTime;
import org.osc.core.broker.model.entities.RoleType;
import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.model.entities.events.Alarm;
import org.osc.core.broker.model.entities.events.AlarmAction;
import org.osc.core.broker.model.entities.events.EventType;
import org.osc.core.broker.model.entities.events.Severity;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.model.entities.job.JobState;
import org.osc.core.broker.model.entities.job.JobStatus;
import org.osc.core.broker.model.entities.job.TaskRecord;
import org.osc.core.broker.model.entities.job.TaskState;
import org.osc.core.broker.model.entities.job.TaskStatus;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.rest.server.NsxAuthFilter;
import org.osc.core.broker.rest.server.VmidcAuthFilter;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.encryption.EncryptionException;

public class DatabaseUtils {
    private static final Logger log = Logger.getLogger(DatabaseUtils.class);

    private static final String DEFAULT_JOB_FAILURE_ALARM_NAME = "Default Job Failure Alarm";
    private static final String DEFAULT_SYSTEM_FAILURE_ALARM_NAME = "Default System Failure Alarm";
    private static final String DEFAULT_DAI_FAILURE_ALARM_NAME = "Default Appliance Instance Failure Alarm";
    public static final String DEFAULT_PASSWORD = "admin123";

    public static void createDefaultDB() {

        log.info("================= Creating default database objects ================");

        Session session = null;
        Transaction tx = null;

        try {
            SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
            session = sessionFactory.getCurrentSession();
            tx = session.beginTransaction();

            createDefaultUsers(session);
            createDefaultAlarms(session);

            tx.commit();

        } catch (Exception ex) {

            log.error("Create DB encountered runtime exception: ", ex);
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        }
    }

    private static void createDefaultUsers(Session session) throws EncryptionException {
        EntityManager<User> userEmgr = new EntityManager<User>(User.class, session);
        User adminUser = userEmgr.findByFieldName("loginName", VmidcAuthFilter.VMIDC_DEFAULT_LOGIN);
        if (adminUser == null) {
            User user = new User();
            user.setLoginName(VmidcAuthFilter.VMIDC_DEFAULT_LOGIN);
            user.setPassword(EncryptionUtil.encryptAESCTR(DEFAULT_PASSWORD));
            user.setRole(RoleType.ADMIN);
            EntityManager.create(session, user);
        }
        User agentUser = userEmgr.findByFieldName("loginName", AgentAuthFilter.VMIDC_AGENT_LOGIN);
        if (agentUser == null) {
            User user = new User();
            user.setLoginName(AgentAuthFilter.VMIDC_AGENT_LOGIN);
            user.setPassword(EncryptionUtil.encryptAESCTR(DEFAULT_PASSWORD));
            user.setRole(RoleType.SYSTEM_AGENT);
            EntityManager.create(session, user);
        }
        User nsxUser = userEmgr.findByFieldName("loginName", NsxAuthFilter.VMIDC_NSX_LOGIN);
        if (nsxUser == null) {
            User user = new User();
            user.setLoginName(NsxAuthFilter.VMIDC_NSX_LOGIN);
            user.setPassword(EncryptionUtil.encryptAESCTR(DEFAULT_PASSWORD));
            user.setRole(RoleType.SYSTEM_NSX);
            EntityManager.create(session, user);
        }
    }

    private static void createDefaultAlarms(Session session) {
        EntityManager<Alarm> alarmEmgr = new EntityManager<Alarm>(Alarm.class, session);
        Alarm alarm = alarmEmgr.findByFieldName("name", DEFAULT_JOB_FAILURE_ALARM_NAME);
        if (alarm == null) {
            Alarm defAlarm = new Alarm();
            defAlarm.setEnable(false);
            defAlarm.setName(DEFAULT_JOB_FAILURE_ALARM_NAME);
            defAlarm.setEventType(EventType.JOB_FAILURE);
            defAlarm.setRegexMatch(".*");
            defAlarm.setSeverity(Severity.LOW);
            defAlarm.setAlarmAction(AlarmAction.NONE);
            EntityManager.create(session, defAlarm);
        }
        alarm = alarmEmgr.findByFieldName("name", DEFAULT_SYSTEM_FAILURE_ALARM_NAME);
        if (alarm == null) {
            Alarm defAlarm = new Alarm();
            defAlarm.setEnable(true);
            defAlarm.setName(DEFAULT_SYSTEM_FAILURE_ALARM_NAME);
            defAlarm.setEventType(EventType.SYSTEM_FAILURE);
            defAlarm.setRegexMatch(".*");
            defAlarm.setSeverity(Severity.HIGH);
            defAlarm.setAlarmAction(AlarmAction.NONE);
            EntityManager.create(session, defAlarm);
        }
        alarm = alarmEmgr.findByFieldName("name", DEFAULT_DAI_FAILURE_ALARM_NAME);
        if (alarm == null) {
            Alarm defAlarm = new Alarm();
            defAlarm.setEnable(false);
            defAlarm.setName(DEFAULT_DAI_FAILURE_ALARM_NAME);
            defAlarm.setEventType(EventType.DAI_FAILURE);
            defAlarm.setRegexMatch(".*");
            defAlarm.setSeverity(Severity.MEDIUM);
            defAlarm.setAlarmAction(AlarmAction.NONE);
            EntityManager.create(session, defAlarm);
        }
    }

    public static void markRunningJobAborted() throws InterruptedException {

        log.info("================= Marking Running jobs as Aborted ================");

        Thread updateJobTaskStateThread = new Thread("UpdateJobThreadState-Thread") {
            @Override
            public void run() {
                Session session = null;
                Transaction tx = null;

                try {
                    SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
                    session = sessionFactory.getCurrentSession();
                    tx = session.beginTransaction();

                    // In case we stopped server while jobs/tasks were running, we'll flagged them all aborted.
                    List<TaskRecord> uncompletedTasks = new TaskEntityMgr(session).getUncompletedTasks();
                    log.info("Marking " + uncompletedTasks.size() + " uncompleted Tasks as aborted");
                    for (TaskRecord task : uncompletedTasks) {
                        task.setState(TaskState.COMPLETED);
                        task.setStatus(TaskStatus.ABORTED);
                        task.setCompletedTimestamp(new DateTime().toDate());
                    }

                    List<JobRecord> uncompletedJobs = new JobEntityManager().getUncompletedJobs(session);
                    log.info("Marking " + uncompletedJobs.size() + " uncompleted Jobs as aborted");
                    for (JobRecord job : uncompletedJobs) {
                        job.setState(JobState.COMPLETED);
                        job.setStatus(JobStatus.ABORTED);
                        job.setFailureReason(VmidcMessages.getString(VmidcMessages_.JOB_ABORT_STARTUP));
                        job.setCompletedTimestamp(new Date());
                    }

                    tx.commit();

                } catch (Exception ex) {

                    log.error("Create DB encountered runtime exception: ", ex);
                    if (tx != null && tx.isActive()) {
                        tx.rollback();
                    }
                }
            }
        };

        updateJobTaskStateThread.start();
        updateJobTaskStateThread.join();
    }
}
