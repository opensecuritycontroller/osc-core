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
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.archive.JobsArchive;
import org.osc.core.broker.model.entities.archive.ThresholdType;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.dto.job.ObjectType;
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

            Connection connection = em.unwrap(Connection.class);

                    try {
                        // calculate threshold date
                        Period period = getPeriod(request);
                        Timestamp sqlTimeString = Timestamp.valueOf(calculateThresholdDate(period, "yyyy-MM-dd HH:mm:ss"));
                        String archiveFileName = calculateThresholdDate(period, "yyyy-MM-dd_HH-mm-ss");

                        String dir = "archive/" + archiveFileName + "/";
                        FileUtils.forceMkdir(new File(dir));


                        //prepare callable statements to export files as .csv
                        List<CallableStatement> cStatements = Arrays.asList(
                                getCallableStatementForJob(connection, sqlTimeString, dir),
                                getCallableStatementJobObject(connection, sqlTimeString, dir),
                                getCallableStatementTask(connection, sqlTimeString, dir),
                                getCallableStatementTaskPredecessor(connection, sqlTimeString, dir),
                                getCallableStatementTaskSuccessor(connection, sqlTimeString, dir),
                                getCallableStatementTaskChild(connection, sqlTimeString, dir),
                                getCallableStatementTaskObject(connection, sqlTimeString, dir),
                                getCallableStatementReleaseInfo(connection, sqlTimeString, dir),
                                getCallableStatementAlert(connection, sqlTimeString, dir)
                        );

                        //prepare statements to clear tables
                        List<PreparedStatement> pStatements = Arrays.asList(
                                getPreparedStatementDeleteTaskObject(connection, sqlTimeString),
                                getPreparedStatementDeleteTaskSuccessor(connection, sqlTimeString),
                                getPreparedStatementDeleteTaskPredecessor(connection, sqlTimeString),
                                getPreparedStatementDeleteTaskChild(connection, sqlTimeString),
                                getPreparedStatementDeleteTask(connection, sqlTimeString),
                                getPreparedStatementDeleteJobObject(connection, sqlTimeString),
                                getPreparedStatementDeleteJob(connection, sqlTimeString),
                                getPreparedStatementDeleteAlert(connection, sqlTimeString)
                        );

                        //Execute callable and then prepared statements
                        for (CallableStatement cStmt : cStatements) {
                            archive(cStmt);
                        }
                        for (PreparedStatement pStmt : pStatements) {
                            delete(pStmt);
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

    private PreparedStatement getPreparedStatementDeleteTaskObject(Connection connection, Timestamp sqlTimeString) throws SQLException {
        PreparedStatement pStmt = connection.prepareStatement("DELETE FROM TASK_OBJECT WHERE task_fk IN "
                + "(SELECT ID FROM TASK WHERE job_fk IN "
                + "(SELECT ID FROM JOB WHERE completed_timestamp <= ?)) ");
        pStmt.setTimestamp(1, sqlTimeString);
        return pStmt;
    }

    private PreparedStatement getPreparedStatementDeleteTaskSuccessor(Connection connection, Timestamp sqlTimeString) throws SQLException {
        PreparedStatement pStmt = connection.prepareStatement("DELETE FROM TASK_SUCCESSOR WHERE task_id IN "
                + "(SELECT ID FROM TASK WHERE job_fk IN "
                + "(SELECT ID FROM JOB WHERE completed_timestamp <= ?)) ");
        pStmt.setTimestamp(1, sqlTimeString);
        return pStmt;
    }

    private PreparedStatement getPreparedStatementDeleteTaskPredecessor(Connection connection, Timestamp sqlTimeString) throws SQLException {
        PreparedStatement pStmt = connection.prepareStatement("DELETE FROM TASK_PREDECESSOR WHERE task_id IN "
                + "(SELECT ID FROM TASK WHERE job_fk IN "
                + "(SELECT ID FROM JOB WHERE completed_timestamp <= ?))");
        pStmt.setTimestamp(1, sqlTimeString);
        return pStmt;
    }

    private PreparedStatement getPreparedStatementDeleteTaskChild(Connection connection, Timestamp sqlTimeString) throws SQLException {
        PreparedStatement pStmt = connection.prepareStatement("DELETE FROM TASK_CHILD WHERE task_id IN "
                + "(SELECT ID FROM TASK WHERE job_fk IN "
                + "(SELECT ID FROM JOB WHERE completed_timestamp <= ?))");
        pStmt.setTimestamp(1, sqlTimeString);
        return pStmt;
    }

    private PreparedStatement getPreparedStatementDeleteTask(Connection connection, Timestamp sqlTimeString) throws SQLException {
        PreparedStatement pStmt = connection.prepareStatement("DELETE FROM TASK WHERE id IN (SELECT ID FROM TASK WHERE job_fk IN "
                + "(SELECT ID FROM JOB WHERE completed_timestamp <= ?))");
        pStmt.setTimestamp(1, sqlTimeString);
        return pStmt;
    }

    private PreparedStatement getPreparedStatementDeleteJobObject(Connection connection, Timestamp sqlTimeString) throws SQLException {
        PreparedStatement pStmt = connection.prepareStatement("DELETE FROM JOB_OBJECT WHERE job_fk IN (SELECT ID FROM JOB WHERE completed_timestamp <= ?)");
        pStmt.setTimestamp(1, sqlTimeString);
        return pStmt;
    }

    private PreparedStatement getPreparedStatementDeleteJob(Connection connection, Timestamp sqlTimeString) throws SQLException {
        PreparedStatement pStmt = connection.prepareStatement("DELETE FROM JOB WHERE completed_timestamp <= ?");
        pStmt.setTimestamp(1, sqlTimeString);
        return pStmt;
    }

    private PreparedStatement getPreparedStatementDeleteAlert(Connection connection, Timestamp sqlTimeString) throws SQLException {
        PreparedStatement pStmt = connection.prepareStatement("DELETE FROM ALERT WHERE created_timestamp <= ?");
        pStmt.setTimestamp(1, sqlTimeString);
        return pStmt;
    }

    private CallableStatement getCallableStatementAlert(Connection connection, Timestamp sqlTimeString, String dir) throws SQLException {
        File cvsFile = new File(dir + "alert.csv");
        CallableStatement cs = connection.prepareCall("{CALL CSVWRITE(?, ?, ?)}");
        cs.setString(1, String.valueOf(cvsFile.getAbsoluteFile()));
        cs.setString(2, "SELECT * FROM ALERT WHERE created_timestamp <= '"+sqlTimeString+"'");
        cs.setString(3,"charset=UTF-8 fieldSeparator=,");
        return cs;
    }

    private CallableStatement getCallableStatementReleaseInfo(Connection connection, Timestamp sqlTimeString, String dir) throws SQLException {
        File cvsFile = new File(dir + "release_info.csv");
        CallableStatement cs = connection.prepareCall("{CALL CSVWRITE(?, ?, ?)}");
        cs.setString(1, String.valueOf(cvsFile.getAbsoluteFile()));
        cs.setString(2, "SELECT * FROM RELEASE_INFO");
        cs.setString(3,"charset=UTF-8 fieldSeparator=,");
        return cs;
    }

    private CallableStatement getCallableStatementTaskObject(Connection connection, Timestamp sqlTimeString, String dir) throws SQLException {
        File cvsFile = new File(dir + "task_object.csv");
        CallableStatement cs = connection.prepareCall("{CALL CSVWRITE(?, ?, ?)}");
        cs.setString(1, String.valueOf(cvsFile.getAbsoluteFile()));
        cs.setString(2, "SELECT * FROM TASK_OBJECT WHERE task_fk IN (SELECT ID FROM TASK WHERE job_fk IN (SELECT ID FROM JOB WHERE completed_timestamp <= '"+sqlTimeString+"'))");
        cs.setString(3,"charset=UTF-8 fieldSeparator=,");
        return cs;
    }

    private CallableStatement getCallableStatementTaskChild(Connection connection, Timestamp sqlTimeString, String dir) throws SQLException {
        File cvsFile = new File(dir + "task_child.csv");
        CallableStatement cs = connection.prepareCall("{CALL CSVWRITE(?, ?, ?)}");
        cs.setString(1, String.valueOf(cvsFile.getAbsoluteFile()));
        cs.setString(2, "SELECT * FROM TASK_CHILD WHERE task_id IN (SELECT ID FROM TASK WHERE job_fk IN (SELECT ID FROM JOB WHERE completed_timestamp <= '"+sqlTimeString+"'))");
        cs.setString(3,"charset=UTF-8 fieldSeparator=,");
        return cs;
    }

    private CallableStatement getCallableStatementTaskSuccessor(Connection connection, Timestamp sqlTimeString, String dir) throws SQLException {
        File cvsFile = new File(dir + "task_successor.csv");
        CallableStatement cs = connection.prepareCall("{CALL CSVWRITE(?, ?, ?)}");
        cs.setString(1, String.valueOf(cvsFile.getAbsoluteFile()));
        cs.setString(2, "SELECT * FROM TASK_SUCCESSOR WHERE task_id IN (SELECT ID FROM TASK WHERE job_fk IN (SELECT ID FROM JOB WHERE completed_timestamp <= '"+sqlTimeString+"'))");
        cs.setString(3,"charset=UTF-8 fieldSeparator=,");
        return cs;
    }

    private CallableStatement getCallableStatementTaskPredecessor(Connection connection, Timestamp sqlTimeString, String dir) throws SQLException {
        File cvsFile = new File(dir + "task_predecessor.csv");
        CallableStatement cs = connection.prepareCall("{CALL CSVWRITE(?, ?, ?)}");
        cs.setString(1, String.valueOf(cvsFile.getAbsoluteFile()));
        cs.setString(2, "SELECT * FROM TASK_PREDECESSOR WHERE task_id IN (SELECT ID FROM TASK WHERE job_fk IN (SELECT ID FROM JOB WHERE completed_timestamp <= '"+sqlTimeString+"'))");
        cs.setString(3,"charset=UTF-8 fieldSeparator=,");
        return cs;
    }

    private CallableStatement getCallableStatementTask(Connection connection, Timestamp sqlTimeString, String dir) throws SQLException {
        File cvsFile = new File(dir + "task.csv");
        CallableStatement cs = connection.prepareCall("{CALL CSVWRITE(?, ?, ?)}");
        cs.setString(1, String.valueOf(cvsFile.getAbsoluteFile()));
        cs.setString(2, "SELECT * FROM TASK WHERE job_fk IN (SELECT ID FROM JOB WHERE completed_timestamp <= '"+sqlTimeString+"')");
        cs.setString(3,"charset=UTF-8 fieldSeparator=,");
        return cs;
    }

    private CallableStatement getCallableStatementJobObject(Connection connection, Timestamp sqlTimeString, String dir) throws SQLException {
        File cvsFile = new File(dir + "job_object.csv");
        CallableStatement cs = connection.prepareCall("{CALL CSVWRITE(?, ?, ?)}");
        cs.setString(1, String.valueOf(cvsFile.getAbsoluteFile()));
        cs.setString(2, "SELECT * FROM JOB_OBJECT WHERE job_fk IN (SELECT ID FROM JOB WHERE completed_timestamp <= '"+sqlTimeString+"')");
        cs.setString(3,"charset=UTF-8 fieldSeparator=,");
        return cs;
    }

    private CallableStatement getCallableStatementForJob(Connection connection, Timestamp sqlTimeString, String dir) throws SQLException {
        File cvsFile = new File(dir + "job.csv");
        CallableStatement cs = connection.prepareCall("{CALL CSVWRITE(?, ?, ?)}");
        cs.setString(1, String.valueOf(cvsFile.getAbsoluteFile()));
        cs.setString(2, "SELECT * FROM JOB WHERE completed_timestamp <= '" + sqlTimeString+"'");
        cs.setString(3,"charset=UTF-8 fieldSeparator=,");
        return cs;
    }

    private void archive(CallableStatement cs) throws SQLException {
        log.info("Execute sql: " + cs.toString());
        try(CallableStatement cStmt = cs) {
            log.info("Rows archived: " + cStmt.executeUpdate());
        }
    }

    private void delete(PreparedStatement ps) throws SQLException {
        log.info("Execute sql: " + ps.toString());
        try(PreparedStatement pStmt = ps) {
            log.info("Rows deleted: " + pStmt.executeUpdate());
        }
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
