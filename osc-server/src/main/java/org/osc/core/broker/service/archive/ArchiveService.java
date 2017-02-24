/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
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
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.Response;
import org.osc.core.util.ArchiveUtil;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

/**
 * Archive service which performs job archive to CSV files.
 */
public class ArchiveService extends ServiceDispatcher<BaseRequest<JobsArchiveDto>, Response> {

    private static final Logger log = Logger.getLogger(ArchiveService.class);

    public ArchiveService() {
    }

    @Override
    public Response exec(final BaseRequest<JobsArchiveDto> request, Session session) throws Exception {
        try {
            JobsArchive jobsArchive = null;

            // Job Archive process could be invoked on demand or on schedule.
            // the request Dto ID suggests which one was invoked.
            if (request.getDto().getId() != null) {
                jobsArchive = (JobsArchive) session.get(JobsArchive.class, 1L);

                // If on schedule job archiving, load parameters from db
                request.getDto().setThresholdUnit(jobsArchive.getThresholdUnit());
                request.getDto().setThresholdValue(jobsArchive.getThresholdValue());
            }

            session.doWork(new Work() {
                @Override
                @SuppressFBWarnings(value="SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
                public void execute(Connection connection) throws SQLException {
                    try {
                        // calculate threshold date
                        Period period = getPeriod(request);
                        String sqlTimeString = calculateThresholdDate(period, "yyyy-MM-dd HH:mm:ss");
                        String archiveFileName = calculateThresholdDate(period, "yyyy-MM-dd_HH-mm-ss");

                        String dir = "archive/" + archiveFileName + "/";
                        FileUtils.forceMkdir(new File(dir));

                        String sql;
                        int rows;
                        try(Statement stmt = connection.createStatement()) {
                            File cvsFile = new File(dir + "job.csv");
                            sql = String.format(
                                    "CALL CSVWRITE('%s', 'SELECT * FROM JOB WHERE completed_timestamp <= ''%s''', "
                                            + " 'charset=UTF-8 fieldSeparator=,');", cvsFile.getAbsoluteFile(),
                                    sqlTimeString);
                            log.info("Execute sql: " + sql);
                            rows = stmt.executeUpdate(sql);
                            log.info("Rows archived: " + rows);

                            cvsFile = new File(dir + "job_object.csv");
                            sql = String.format("CALL CSVWRITE('%s', 'SELECT * FROM JOB_OBJECT WHERE job_fk IN "
                                    + "(SELECT ID FROM JOB WHERE completed_timestamp <= ''%s'')', "
                                    + " 'charset=UTF-8 fieldSeparator=,');", cvsFile.getAbsoluteFile(), sqlTimeString);
                            log.info("Execute sql: " + sql);
                            rows = stmt.executeUpdate(sql);
                            log.info("Rows archived: " + rows);

                            cvsFile = new File(dir + "task.csv");
                            sql = String.format("CALL CSVWRITE('%s', 'SELECT * FROM TASK WHERE job_fk IN "
                                    + "(SELECT ID FROM JOB WHERE completed_timestamp <= ''%s'')', "
                                    + " 'charset=UTF-8 fieldSeparator=,');", cvsFile.getAbsoluteFile(), sqlTimeString);
                            log.info("Execute sql: " + sql);
                            rows = stmt.executeUpdate(sql);
                            log.info("Rows archived: " + rows);

                            cvsFile = new File(dir + "task_predecessor.csv");
                            sql = String.format("CALL CSVWRITE('%s', 'SELECT * FROM TASK_PREDECESSOR WHERE task_id IN "
                                    + "(SELECT ID FROM TASK WHERE job_fk IN "
                                    + "(SELECT ID FROM JOB WHERE completed_timestamp <= ''%s''))', "
                                    + " 'charset=UTF-8 fieldSeparator=,');", cvsFile.getAbsoluteFile(), sqlTimeString);
                            log.info("Execute sql: " + sql);
                            rows = stmt.executeUpdate(sql);
                            log.info("Rows archived: " + rows);

                            cvsFile = new File(dir + "task_successor.csv");
                            sql = String.format("CALL CSVWRITE('%s', 'SELECT * FROM TASK_SUCCESSOR WHERE task_id IN "
                                    + "(SELECT ID FROM TASK WHERE job_fk IN "
                                    + "(SELECT ID FROM JOB WHERE completed_timestamp <= ''%s''))', "
                                    + " 'charset=UTF-8 fieldSeparator=,');", cvsFile.getAbsoluteFile(), sqlTimeString);
                            log.info("Execute sql: " + sql);
                            rows = stmt.executeUpdate(sql);
                            log.info("Rows archived: " + rows);

                            cvsFile = new File(dir + "task_child.csv");
                            sql = String.format("CALL CSVWRITE('%s', 'SELECT * FROM TASK_CHILD WHERE task_id IN "
                                    + "(SELECT ID FROM TASK WHERE job_fk IN "
                                    + "(SELECT ID FROM JOB WHERE completed_timestamp <= ''%s''))', "
                                    + " 'charset=UTF-8 fieldSeparator=,');", cvsFile.getAbsoluteFile(), sqlTimeString);
                            log.info("Execute sql: " + sql);
                            rows = stmt.executeUpdate(sql);
                            log.info("Rows archived: " + rows);

                            cvsFile = new File(dir + "task_object.csv");
                            sql = String.format("CALL CSVWRITE('%s', 'SELECT * FROM TASK_OBJECT WHERE task_fk IN "
                                    + "(SELECT ID FROM TASK WHERE job_fk IN "
                                    + "(SELECT ID FROM JOB WHERE completed_timestamp <= ''%s''))', "
                                    + " 'charset=UTF-8 fieldSeparator=,');", cvsFile.getAbsoluteFile(), sqlTimeString);
                            log.info("Execute sql: " + sql);
                            rows = stmt.executeUpdate(sql);
                            log.info("Rows archived: " + rows);

                            // db version for future meta data usage
                            cvsFile = new File(dir + "release_info.csv");
                            sql = String.format(
                                    "CALL CSVWRITE('%s', 'SELECT * FROM RELEASE_INFO', 'charset=UTF-8 fieldSeparator=,');",
                                    cvsFile.getAbsoluteFile());
                            log.info("Execute sql: " + sql);
                            rows = stmt.executeUpdate(sql);
                            log.info("Rows archived: " + rows);

                            // Archive alerts
                            cvsFile = new File(dir + "alert.csv");
                            sql = String.format(
                                    "CALL CSVWRITE('%s', 'SELECT * FROM ALERT WHERE created_timestamp <= ''%s''', "
                                            + " 'charset=UTF-8 fieldSeparator=,');", cvsFile.getAbsoluteFile(),
                                    sqlTimeString);
                            log.info("Execute sql: " + sql);
                            rows = stmt.executeUpdate(sql);
                            log.info("Rows archived: " + rows);

                            sql = String.format("DELETE FROM TASK_OBJECT WHERE task_fk IN "
                                    + "(SELECT ID FROM TASK WHERE job_fk IN "
                                    + "(SELECT ID FROM JOB WHERE completed_timestamp <= '%s')) ", sqlTimeString);
                            log.info("Execute sql: " + sql);
                            stmt.execute(sql);

                            sql = String.format("DELETE FROM TASK_SUCCESSOR WHERE task_id IN "
                                    + "(SELECT ID FROM TASK WHERE job_fk IN "
                                    + "(SELECT ID FROM JOB WHERE completed_timestamp <= '%s')) ", sqlTimeString);
                            log.info("Execute sql: " + sql);
                            stmt.execute(sql);

                            sql = String.format("DELETE FROM TASK_PREDECESSOR WHERE task_id IN "
                                    + "(SELECT ID FROM TASK WHERE job_fk IN "
                                    + "(SELECT ID FROM JOB WHERE completed_timestamp <= '%s')) ", sqlTimeString);
                            log.info("Execute sql: " + sql);
                            stmt.execute(sql);

                            sql = String.format("DELETE FROM TASK_CHILD WHERE task_id IN "
                                    + "(SELECT ID FROM TASK WHERE job_fk IN "
                                    + "(SELECT ID FROM JOB WHERE completed_timestamp <= '%s')) ", sqlTimeString);
                            log.info("Execute sql: " + sql);
                            stmt.execute(sql);

                            sql = String.format("DELETE FROM TASK WHERE id IN (SELECT ID FROM TASK WHERE job_fk IN "
                                    + "(SELECT ID FROM JOB WHERE completed_timestamp <= '%s')) ", sqlTimeString);
                            log.info("Execute sql: " + sql);
                            stmt.execute(sql);

                            sql = String
                                    .format("DELETE FROM JOB_OBJECT WHERE job_fk IN (SELECT ID FROM JOB WHERE completed_timestamp <= '"
                                            + sqlTimeString + "')");
                            log.info("Execute sql: " + sql);
                            stmt.execute(sql);

                            sql = String.format("DELETE FROM JOB WHERE completed_timestamp <= '" + sqlTimeString + "'");
                            log.info("Execute sql: " + sql);
                            stmt.execute(sql);

                            sql = String.format("DELETE FROM ALERT WHERE created_timestamp <= '" + sqlTimeString + "'");
                            log.info("Execute sql: " + sql);
                            stmt.execute(sql);

                            ArchiveUtil.archive(dir, "archive/osc-archive-" + archiveFileName + ".zip");
                            FileUtils.deleteDirectory(new File(dir));
                        }
                    } catch (Exception e) {
                        log.error("Error while archiving jobs", e);
                        AlertGenerator.processSystemFailureEvent(SystemFailureType.ARCHIVE_FAILURE,
                                new LockObjectReference(1L, "Archive Settings", ObjectType.ARCHIVE),
                                "Failure during archiving operation " + e.getMessage());
                    }

                }
            });

            if (jobsArchive != null) {
                jobsArchive.setLastTriggerTimestamp(new Date());
                EntityManager.update(session, jobsArchive);
            }

        } catch (Exception e) {
            log.error("Scheduler fail to start/stop Archiving job", e);
        }

        return new Response() {
        };
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
