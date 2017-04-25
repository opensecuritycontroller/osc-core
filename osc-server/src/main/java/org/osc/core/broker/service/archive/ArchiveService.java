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
package org.osc.core.broker.service.archive;

import java.io.File;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockObjectReference.ObjectType;
import org.osc.core.broker.model.entities.archive.JobsArchive;
import org.osc.core.broker.model.entities.archive.ThresholdType;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.Response;
import org.osc.core.util.ArchiveUtil;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Archive service which performs job archive to CSV files.
 */
public class ArchiveService extends ServiceDispatcher<BaseRequest<JobsArchiveDto>, Response> {

    private static final Logger log = Logger.getLogger(ArchiveService.class);

    public ArchiveService() {
    }

    @Override
    @SuppressFBWarnings(value="SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
    public Response exec(final BaseRequest<JobsArchiveDto> request, EntityManager em) throws Exception {
        try {
            JobsArchive jobsArchive = null;

            // Job Archive process could be invoked on demand or on schedule.
            // the request Dto ID suggests which one was invoked.
            if (request.getDto().getId() != null) {
                jobsArchive = em.find(JobsArchive.class, 1L);

                // If on schedule job archiving, load parameters from db
                request.getDto().setThresholdUnit(jobsArchive.getThresholdUnit());
                request.getDto().setThresholdValue(jobsArchive.getThresholdValue());
            }

                    try {
                        // calculate threshold date
                        Period period = getPeriod(request);
                        Timestamp sqlTimeString = Timestamp.valueOf(calculateThresholdDate(period, "yyyy-MM-dd HH:mm:ss"));
                        String archiveFileName = calculateThresholdDate(period, "yyyy-MM-dd_HH-mm-ss");

                        String dir = "archive/" + archiveFileName + "/";
                        FileUtils.forceMkdir(new File(dir));


                        //prepare callable statements to export files as .csv
                        List<Query> cStatements = Arrays.asList(
                                getCallableStatementForJob(em, sqlTimeString, dir),
                                getCallableStatementJobObject(em, sqlTimeString, dir),
                                getCallableStatementTask(em, sqlTimeString, dir),
                                getCallableStatementTaskPredecessor(em, sqlTimeString, dir),
                                getCallableStatementTaskSuccessor(em, sqlTimeString, dir),
                                getCallableStatementTaskChild(em, sqlTimeString, dir),
                                getCallableStatementTaskObject(em, sqlTimeString, dir),
                                getCallableStatementReleaseInfo(em, sqlTimeString, dir),
                                getCallableStatementAlert(em, sqlTimeString, dir)
                        );

                        //prepare statements to clear tables
                        List<Query> pStatements = Arrays.asList(
                                getPreparedStatementDeleteTaskObject(em, sqlTimeString),
                                getPreparedStatementDeleteTaskSuccessor(em, sqlTimeString),
                                getPreparedStatementDeleteTaskPredecessor(em, sqlTimeString),
                                getPreparedStatementDeleteTaskChild(em, sqlTimeString),
                                getPreparedStatementDeleteTask(em, sqlTimeString),
                                getPreparedStatementDeleteJobObject(em, sqlTimeString),
                                getPreparedStatementDeleteJob(em, sqlTimeString),
                                getPreparedStatementDeleteAlert(em, sqlTimeString)
                        );

                        //Execute callable and then prepared statements
                        for (Query spq : cStatements) {
                            archive(spq);
                        }
                        for (Query q : pStatements) {
                            delete(q);
                        }

                        ArchiveUtil.archive(dir, "archive/osc-archive-" + archiveFileName + ".zip");
                        FileUtils.deleteDirectory(new File(dir));

                    } catch (Exception e) {
                        log.error("Error while archiving jobs", e);
                        AlertGenerator.processSystemFailureEvent(SystemFailureType.ARCHIVE_FAILURE,
                                new LockObjectReference(1L, "Archive Settings", ObjectType.ARCHIVE),
                                "Failure during archiving operation " + e.getMessage());
                    }


            if (jobsArchive != null) {
                jobsArchive.setLastTriggerTimestamp(new Date());
                OSCEntityManager.update(em, jobsArchive);
            }

        } catch (Exception e) {
            log.error("Scheduler fail to start/stop Archiving job", e);
        }

        return new Response() {
        };
    }

    private Query getPreparedStatementDeleteTaskObject(EntityManager em, Timestamp sqlTimeString) throws SQLException {
        Query query = em.createNativeQuery("DELETE FROM TASK_OBJECT WHERE task_fk IN "
                + "(SELECT ID FROM TASK WHERE job_fk IN "
                + "(SELECT ID FROM JOB WHERE completed_timestamp <= ?)) ");
        query.setParameter(1, sqlTimeString);
        return query;
    }

    private Query getPreparedStatementDeleteTaskSuccessor(EntityManager em, Timestamp sqlTimeString) throws SQLException {
        Query query = em.createNativeQuery("DELETE FROM TASK_SUCCESSOR WHERE task_id IN "
                + "(SELECT ID FROM TASK WHERE job_fk IN "
                + "(SELECT ID FROM JOB WHERE completed_timestamp <= ?)) ");
        query.setParameter(1, sqlTimeString);
        return query;
    }

    private Query getPreparedStatementDeleteTaskPredecessor(EntityManager em, Timestamp sqlTimeString) throws SQLException {
        Query query = em.createNativeQuery("DELETE FROM TASK_PREDECESSOR WHERE task_id IN "
                + "(SELECT ID FROM TASK WHERE job_fk IN "
                + "(SELECT ID FROM JOB WHERE completed_timestamp <= ?))");
        query.setParameter(1, sqlTimeString);
        return query;
    }

    private Query getPreparedStatementDeleteTaskChild(EntityManager em, Timestamp sqlTimeString) throws SQLException {
        Query query = em.createNativeQuery("DELETE FROM TASK_CHILD WHERE task_id IN "
                + "(SELECT ID FROM TASK WHERE job_fk IN "
                + "(SELECT ID FROM JOB WHERE completed_timestamp <= ?))");
        query.setParameter(1, sqlTimeString);
        return query;
    }

    private Query getPreparedStatementDeleteTask(EntityManager em, Timestamp sqlTimeString) throws SQLException {
        Query query = em.createNativeQuery("DELETE FROM TASK WHERE id IN (SELECT ID FROM TASK WHERE job_fk IN "
                + "(SELECT ID FROM JOB WHERE completed_timestamp <= ?))");
        query.setParameter(1, sqlTimeString);
        return query;
    }

    private Query getPreparedStatementDeleteJobObject(EntityManager em, Timestamp sqlTimeString) throws SQLException {
        Query query = em.createNativeQuery("DELETE FROM JOB_OBJECT WHERE job_fk IN (SELECT ID FROM JOB WHERE completed_timestamp <= ?)");
        query.setParameter(1, sqlTimeString);
        return query;
    }

    private Query getPreparedStatementDeleteJob(EntityManager em, Timestamp sqlTimeString) throws SQLException {
        Query query = em.createNativeQuery("DELETE FROM JOB WHERE completed_timestamp <= ?");
        query.setParameter(1, sqlTimeString);
        return query;
    }

    private Query getPreparedStatementDeleteAlert(EntityManager em, Timestamp sqlTimeString) throws SQLException {
        Query query = em.createNativeQuery("DELETE FROM ALERT WHERE created_timestamp <= ?");
        query.setParameter(1, sqlTimeString);
        return query;
    }

    private Query getCallableStatementAlert(EntityManager em, Timestamp sqlTimeString, String dir) throws SQLException {
        File cvsFile = new File(dir + "alert.csv");
        Query spq = em.createNativeQuery("{CALL CSVWRITE(?, ?, ?)}");
        spq.setParameter(1, String.valueOf(cvsFile.getAbsoluteFile()));
        spq.setParameter(2, "SELECT * FROM ALERT WHERE created_timestamp <= '"+sqlTimeString+"'");
        spq.setParameter(3,"charset=UTF-8 fieldSeparator=,");
        return spq;
    }

    private Query getCallableStatementReleaseInfo(EntityManager em, Timestamp sqlTimeString, String dir) throws SQLException {
        File cvsFile = new File(dir + "release_info.csv");
        Query spq = em.createNativeQuery("{CALL CSVWRITE(?, ?, ?)}");
        spq.setParameter(1, String.valueOf(cvsFile.getAbsoluteFile()));
        spq.setParameter(2, "SELECT * FROM RELEASE_INFO");
        spq.setParameter(3,"charset=UTF-8 fieldSeparator=,");
        return spq;
    }

    private Query getCallableStatementTaskObject(EntityManager em, Timestamp sqlTimeString, String dir) throws SQLException {
        File cvsFile = new File(dir + "task_object.csv");
        Query spq = em.createNativeQuery("{CALL CSVWRITE(?, ?, ?)}");
        spq.setParameter(1, String.valueOf(cvsFile.getAbsoluteFile()));
        spq.setParameter(2, "SELECT * FROM TASK_OBJECT WHERE task_fk IN (SELECT ID FROM TASK WHERE job_fk IN (SELECT ID FROM JOB WHERE completed_timestamp <= '"+sqlTimeString+"'))");
        spq.setParameter(3,"charset=UTF-8 fieldSeparator=,");
        return spq;
    }

    private Query getCallableStatementTaskChild(EntityManager em, Timestamp sqlTimeString, String dir) throws SQLException {
        File cvsFile = new File(dir + "task_child.csv");
        Query spq = em.createNativeQuery("{CALL CSVWRITE(?, ?, ?)}");
        spq.setParameter(1, String.valueOf(cvsFile.getAbsoluteFile()));
        spq.setParameter(2, "SELECT * FROM TASK_CHILD WHERE task_id IN (SELECT ID FROM TASK WHERE job_fk IN (SELECT ID FROM JOB WHERE completed_timestamp <= '"+sqlTimeString+"'))");
        spq.setParameter(3,"charset=UTF-8 fieldSeparator=,");
        return spq;
    }

    private Query getCallableStatementTaskSuccessor(EntityManager em, Timestamp sqlTimeString, String dir) throws SQLException {
        File cvsFile = new File(dir + "task_successor.csv");
        Query spq = em.createNativeQuery("{CALL CSVWRITE(?, ?, ?)}");
        spq.setParameter(1, String.valueOf(cvsFile.getAbsoluteFile()));
        spq.setParameter(2, "SELECT * FROM TASK_SUCCESSOR WHERE task_id IN (SELECT ID FROM TASK WHERE job_fk IN (SELECT ID FROM JOB WHERE completed_timestamp <= '"+sqlTimeString+"'))");
        spq.setParameter(3,"charset=UTF-8 fieldSeparator=,");
        return spq;
    }

    private Query getCallableStatementTaskPredecessor(EntityManager em, Timestamp sqlTimeString, String dir) throws SQLException {
        File cvsFile = new File(dir + "task_predecessor.csv");
        Query spq = em.createNativeQuery("{CALL CSVWRITE(?, ?, ?)}");
        spq.setParameter(1, String.valueOf(cvsFile.getAbsoluteFile()));
        spq.setParameter(2, "SELECT * FROM TASK_PREDECESSOR WHERE task_id IN (SELECT ID FROM TASK WHERE job_fk IN (SELECT ID FROM JOB WHERE completed_timestamp <= '"+sqlTimeString+"'))");
        spq.setParameter(3,"charset=UTF-8 fieldSeparator=,");
        return spq;
    }

    private Query getCallableStatementTask(EntityManager em, Timestamp sqlTimeString, String dir) throws SQLException {
        File cvsFile = new File(dir + "task.csv");
        Query spq = em.createNativeQuery("{CALL CSVWRITE(?, ?, ?)}");
        spq.setParameter(1, String.valueOf(cvsFile.getAbsoluteFile()));
        spq.setParameter(2, "SELECT * FROM TASK WHERE job_fk IN (SELECT ID FROM JOB WHERE completed_timestamp <= '"+sqlTimeString+"')");
        spq.setParameter(3,"charset=UTF-8 fieldSeparator=,");
        return spq;
    }

    private Query getCallableStatementJobObject(EntityManager em, Timestamp sqlTimeString, String dir) throws SQLException {
        File cvsFile = new File(dir + "job_object.csv");
        Query spq = em.createNativeQuery("{CALL CSVWRITE(?, ?, ?)}");
        spq.setParameter(1, String.valueOf(cvsFile.getAbsoluteFile()));
        spq.setParameter(2, "SELECT * FROM JOB_OBJECT WHERE job_fk IN (SELECT ID FROM JOB WHERE completed_timestamp <= '"+sqlTimeString+"')");
        spq.setParameter(3,"charset=UTF-8 fieldSeparator=,");
        return spq;
    }

    private Query getCallableStatementForJob(EntityManager em, Timestamp sqlTimeString, String dir) throws SQLException {
        File cvsFile = new File(dir + "job.csv");
        Query spq = em.createNativeQuery("{CALL CSVWRITE(?, ?, ?)}");
        spq.setParameter(1, String.valueOf(cvsFile.getAbsoluteFile()));
        spq.setParameter(2, "SELECT * FROM JOB WHERE completed_timestamp <= '" + sqlTimeString+"'");
        spq.setParameter(3,"charset=UTF-8 fieldSeparator=,");
        return spq;
    }

    private void archive(Query spq) throws SQLException {
        log.info("Execute sql: " + spq.toString());
        log.info("Rows archived: " + spq.executeUpdate());
    }

    private void delete(Query q) throws SQLException {
        log.info("Execute sql: " + q.toString());
        log.info("Rows deleted: " + q.executeUpdate());
    }

    private String calculateThresholdDate(Period period, String datePattern){
        DateTime thresholdDate = new DateTime().minus(period);
        DateTimeFormatter fmt = DateTimeFormat.forPattern(datePattern);
        return fmt.print(thresholdDate);
    }

    private Period getPeriod(BaseRequest<JobsArchiveDto> request) {
        Period period;
        if (request.getDto().getThresholdUnit().equals(ThresholdType.MONTHS)) {
            period = Period.months(request.getDto().getThresholdValue());
        } else {
            period = Period.years(request.getDto().getThresholdValue());
        }
        return period;
    }

}
