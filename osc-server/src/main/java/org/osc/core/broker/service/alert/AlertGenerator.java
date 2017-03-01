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
package org.osc.core.broker.service.alert;

import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.JobStatus;
import org.osc.core.broker.job.Job.JobCompletionListener;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockObjectReference.ObjectType;
import org.osc.core.broker.model.entities.events.AcknowledgementStatus;
import org.osc.core.broker.model.entities.events.Alarm;
import org.osc.core.broker.model.entities.events.AlarmAction;
import org.osc.core.broker.model.entities.events.Alert;
import org.osc.core.broker.model.entities.events.DaiFailureType;
import org.osc.core.broker.model.entities.events.EmailSettings;
import org.osc.core.broker.model.entities.events.EventType;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.service.email.EmailSettingsDto;
import org.osc.core.broker.service.persistence.AlertEntityMgr;
import org.osc.core.broker.service.persistence.EmailSettingsEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.util.EmailUtil;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.HibernateUtil;

public class AlertGenerator implements JobCompletionListener {

    private static final Logger log = Logger.getLogger(AlertGenerator.class);
    private static final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    @Override
    public void completed(Job job) {

        //No need to do anything if the job completion status is not "FAILED"
        if (job.getStatus().equals(JobStatus.FAILED)) {
            processJobFailureEvent(job);
        }
    }

    public static void processJobFailureEvent(Job job) {
        processFailureEvent(job, null, null, new LockObjectReference(job), job.getFailureReason());
    }

    public static void processSystemFailureEvent(SystemFailureType systemFailureType, String message) {
        processFailureEvent(null, systemFailureType, null, null, message);
    }

    public static void processSystemFailureEvent(SystemFailureType systemFailureType, LockObjectReference object,
            String message) {
        processFailureEvent(null, systemFailureType, null, object, message);
    }

    public static void processDaiFailureEvent(DaiFailureType daiFailureType, LockObjectReference object, String message) {
        processFailureEvent(null, null, daiFailureType, object, message);
    }

    private static void processFailureEvent(Job job, SystemFailureType systemFailureType,
            DaiFailureType daiFailureType, LockObjectReference object, String message) {

        Session session = null;
        Transaction tx = null;

        try {

            session = sessionFactory.openSession();
            tx = session.beginTransaction();

            EntityManager<Alarm> emgr = new EntityManager<Alarm>(Alarm.class, session);

            //Iterate all the alarms and look for a regex match. As of now we only support Job Failure event type.
            //Generate alert for all the matches and perform the defined alarm action if any!
            for (Alarm alarm : emgr.listAll()) {

                if (alarm.isEnabled()) {
                    switch (alarm.getEventType()) {
                    case JOB_FAILURE:
                        if (job != null) {
                            if (isMatchAlarm(alarm.getRegexMatch(), job.getName())) {
                                generateAlertAndNotify(session, alarm, object, message);
                            }
                        }
                        break;
                    case SYSTEM_FAILURE:
                        // systemFailureType can only be null if it is email failure since email failure
                        // is in fact handled in processEmailFailure method as a special case
                        if (systemFailureType != null && isMatchAlarm(alarm.getRegexMatch(), message)) {
                            generateAlertAndNotify(session, alarm, object, message);
                        }
                        break;
                    case DAI_FAILURE:
                        if (daiFailureType != null && isMatchAlarm(alarm.getRegexMatch(), object.getName())) {
                            generateAlertAndNotify(session, alarm, object, message);
                        }
                        break;
                    default:
                        log.error("The alarm event type " + alarm.getEventType() + " is not supported");
                    }
                }
            }

            tx.commit();
            TransactionalBroadcastUtil.broadcast(session);
        } catch (Exception ex) {
            log.error("Failed to finish processing the job failure event : ", ex);
            if (tx != null) {
                tx.rollback();
                TransactionalBroadcastUtil.removeSessionFromMap(session);
            }
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    private static boolean isMatchAlarm(String regexMatch, String match) {
        Pattern p = Pattern.compile(regexMatch);
        return p.matcher(match).matches();
    }

    private static void generateAlertAndNotify(Session session, Alarm alarm, LockObjectReference object, String message) {

        try {
            Alert alert = generateAlert(session, alarm, object, message);
            if (alarm.getAlarmAction().equals(AlarmAction.EMAIL)) {
                sendEmail(session, alarm, alert, message);
            }
        } catch (Exception ex) {
            log.error("Failed to generate alert: ", ex);
        }
    }

    private static Alert generateAlert(Session session, Alarm alarm, LockObjectReference object, String message)
            throws Exception {

        BaseRequest<AlertDto> request = new BaseRequest<AlertDto>();

        request.setDto(new AlertDto());
        request.getDto().setName(alarm.getName());
        request.getDto().setEventType(alarm.getEventType());
        request.getDto().setObject(object);
        request.getDto().setSeverity(alarm.getSeverity());
        request.getDto().setStatus(AcknowledgementStatus.PENDING_ACKNOWLEDGEMENT);
        request.getDto().setMessage(message);

        AlertDto.checkForNullFields(request.getDto());

        Alert alert = AlertEntityMgr.createEntity(request.getDto());

        // creating new entry in the db using entity manager object
        alert = EntityManager.create(session, alert);

        log.info("Adding new alert - " + alert.getName());

        return alert;
    }

    private static void processEmailFailure(Session session, LockObjectReference object,
            SystemFailureType systemFailureType, String message) {
        EntityManager<Alarm> emgr = new EntityManager<Alarm>(Alarm.class, session);
        for (Alarm alarm : emgr.listAll()) {
            if (alarm.isEnabled() && alarm.getEventType().equals(EventType.SYSTEM_FAILURE)
                    && isMatchAlarm(alarm.getRegexMatch(), systemFailureType.toString())) {
                try {
                    // Since the sole purpose here is to notify the user of email misconfiguration/problem
                    // as soon as a match alarm is found, generate ONE alert and break from the loop
                    generateAlert(session, alarm, object, message);
                    break;
                } catch (Exception ex) {
                    log.error("Failed to generate email failure system alert: " + ex);
                }
            }
        }
    }

    private static void sendEmail(Session session, Alarm alarm, Alert alert, String message) {

        EmailSettings emailSettings = (EmailSettings) session.get(EmailSettings.class, 1L);

        try {
            if (emailSettings != null) {
                EmailSettingsDto dto = new EmailSettingsDto();
                EmailSettingsEntityMgr.fromEntity(emailSettings, dto);

                EmailUtil.sendEmail(dto.getMailServer(), dto.getPort(), dto.getEmailId(), dto.getPassword(),
                        alarm.getReceipientEmail(), alert);
            }
        } catch (Exception ex) {
            log.error("Failed to send email to the interested user(s): ", ex);
            processEmailFailure(session, new LockObjectReference(1L, "Email Settings", ObjectType.EMAIL),
                    SystemFailureType.EMAIL_FAILURE, "Fail to send alert email.");
        }
    }
}
