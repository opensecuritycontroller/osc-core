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

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
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
import org.osc.core.broker.rest.server.OscAuthFilter;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.encryption.EncryptionException;
import org.osgi.service.transaction.control.ScopedWorkException;

public class DatabaseUtils {
    private static final Logger log = Logger.getLogger(DatabaseUtils.class);

    private static final String DEFAULT_JOB_FAILURE_ALARM_NAME = "Default Job Failure Alarm";
    private static final String DEFAULT_SYSTEM_FAILURE_ALARM_NAME = "Default System Failure Alarm";
    private static final String DEFAULT_DAI_FAILURE_ALARM_NAME = "Default Appliance Instance Failure Alarm";
    public static final String DEFAULT_PASSWORD = "admin123";

    public static void createDefaultDB() {

        log.info("================= Creating default database objects ================");

        try {
            EntityManager em = HibernateUtil.getTransactionalEntityManager();
            HibernateUtil.getTransactionControl().required(() -> {
                createDefaultUsers(em);
                createDefaultAlarms(em);
                return null;
            });
        } catch (Exception ex) {
            log.error("Create DB encountered runtime exception: ", ex);
        }
    }

    private static void createDefaultUsers(EntityManager em) throws EncryptionException {
        OSCEntityManager<User> userEmgr = new OSCEntityManager<User>(User.class, em);
        User adminUser = userEmgr.findByFieldName("loginName", OscAuthFilter.OSC_DEFAULT_LOGIN);
        if (adminUser == null) {
            User user = new User();
            user.setLoginName(OscAuthFilter.OSC_DEFAULT_LOGIN);
            user.setPassword(EncryptionUtil.encryptAESCTR(DEFAULT_PASSWORD));
            user.setRole(RoleType.ADMIN);
            OSCEntityManager.create(em, user);
        }
    }

    private static void createDefaultAlarms(EntityManager em) {
        OSCEntityManager<Alarm> alarmEmgr = new OSCEntityManager<Alarm>(Alarm.class, em);
        Alarm alarm = alarmEmgr.findByFieldName("name", DEFAULT_JOB_FAILURE_ALARM_NAME);
        if (alarm == null) {
            Alarm defAlarm = new Alarm();
            defAlarm.setEnable(false);
            defAlarm.setName(DEFAULT_JOB_FAILURE_ALARM_NAME);
            defAlarm.setEventType(EventType.JOB_FAILURE);
            defAlarm.setRegexMatch(".*");
            defAlarm.setSeverity(Severity.LOW);
            defAlarm.setAlarmAction(AlarmAction.NONE);
            OSCEntityManager.create(em, defAlarm);
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
            OSCEntityManager.create(em, defAlarm);
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
            OSCEntityManager.create(em, defAlarm);
        }
    }

    public static void markRunningJobAborted() throws InterruptedException {

        log.info("================= Marking Running jobs as Aborted ================");

        Thread updateJobTaskStateThread = new Thread("UpdateJobThreadState-Thread") {
            @Override
            public void run() {

                try {
                    EntityManager em = HibernateUtil.getTransactionalEntityManager();
                    HibernateUtil.getTransactionControl().required(() -> {

                        // In case we stopped server while jobs/tasks were running, we'll flagged them all aborted.
                        List<TaskRecord> uncompletedTasks = new TaskEntityMgr(em).getUncompletedTasks();
                        log.info("Marking " + uncompletedTasks.size() + " uncompleted Tasks as aborted");
                        for (TaskRecord task : uncompletedTasks) {
                            task.setState(TaskState.COMPLETED);
                            task.setStatus(TaskStatus.ABORTED);
                            task.setCompletedTimestamp(new DateTime().toDate());
                        }

                        List<JobRecord> uncompletedJobs = new JobEntityManager().getUncompletedJobs(em);
                        log.info("Marking " + uncompletedJobs.size() + " uncompleted Jobs as aborted");
                        for (JobRecord job : uncompletedJobs) {
                            job.setState(JobState.COMPLETED);
                            job.setStatus(JobStatus.ABORTED);
                            job.setFailureReason(VmidcMessages.getString(VmidcMessages_.JOB_ABORT_STARTUP));
                            job.setCompletedTimestamp(new Date());
                        }
                        return null;
                    });
                } catch (ScopedWorkException ex) {
                    log.error("Create DB encountered runtime exception: ", ex.getCause());
                } catch (Exception ex) {
                    log.error("Create DB encountered runtime exception: ", ex);
                }
            }
        };

        updateJobTaskStateThread.start();
        updateJobTaskStateThread.join();
    }
}
