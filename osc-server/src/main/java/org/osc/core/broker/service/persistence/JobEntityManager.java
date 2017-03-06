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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.osc.core.broker.job.JobState;
import org.osc.core.broker.job.JobStatus;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockObjectReference.ObjectType;
import org.osc.core.broker.model.entities.job.JobObject;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.service.dto.JobRecordDto;
import org.osc.core.broker.util.db.HibernateUtil;

public class JobEntityManager {

    public static void fromEntity(JobRecord job, JobRecordDto dto) {
        // transform from entity to dto
        dto.setId(job.getId());
        dto.setName(job.getName());
        dto.setFailureReason(job.getFailureReason());
        dto.setState(JobState.valueOf(job.getState().name()));
        dto.setStatus(JobStatus.valueOf(job.getStatus().name()));
        dto.setQueued(job.getQueuedTimestamp());
        dto.setStarted(job.getStartedTimestamp());
        dto.setCompleted(job.getCompletedTimestamp());
        dto.setObjects(getJobObjects(job));
        dto.setSubmittedBy(job.getSubmittedBy());
    }

    private static Set<LockObjectReference> getJobObjects(JobRecord job) {
        if (job.getObjects() == null) {
            return null;
        }

        Set<LockObjectReference> objects = new HashSet<LockObjectReference>();
        for (JobObject jo : job.getObjects()) {
            objects.add(new LockObjectReference(jo.getObjectId(), jo.getName(),
                    ObjectType.valueOf(jo.getObjectType().name())));
        }
        return objects;
    }

    public static Long getTaskCount(Long jobId) {

        Session session = HibernateUtil.getSessionFactory().openSession();

        try {
            Transaction tx = session.beginTransaction();

            String hql = "SELECT count(*) FROM TaskRecord WHERE job_fk = :jobId";

            Query query = session.createQuery(hql);
            query.setParameter("jobId", jobId);
            query.setFirstResult(0).setMaxResults(1).uniqueResult();

            Long count = (Long) query.list().get(0);
            tx.commit();

            return count;

        } finally {

            if (session != null) {
                session.close();
            }
        }

    }

    public static Long getCompletedTaskCount(Long jobId) {

        Session session = HibernateUtil.getSessionFactory().openSession();

        try {
            Transaction tx = session.beginTransaction();

            String hql = "SELECT count(*) FROM TaskRecord WHERE job_fk = :jobId AND state = 'COMPLETED'";

            Query query = session.createQuery(hql);
            query.setParameter("jobId", jobId);
            query.setFirstResult(0).setMaxResults(1).uniqueResult();

            Long count = (Long) query.list().get(0);
            tx.commit();

            return count;

        } finally {

            if (session != null) {
                session.close();
            }
        }
    }

    /**
     * Gets the total job count from the database
     * @param session2
     *
     * @return total job count
     */
    public static Long getTotalJobCount(Session session) {

        Long totalJobCount = (Long) session.createCriteria(JobRecord.class).setProjection(Projections.rowCount())
                .uniqueResult();

        return totalJobCount;

    }

    /**
     * Gets the total failed job count from the database
     *
     * @return failed job count
     */
    public static Long getTotalJobFailCount(Session session) {
        Long totalJobFailCount = (Long) session.createCriteria(JobRecord.class)
                .add(Restrictions.eq("status", JobStatus.FAILED)).setProjection(Projections.rowCount()).uniqueResult();

        return totalJobFailCount;
    }

    @SuppressWarnings("unchecked")
    public List<JobRecord> getUncompletedJobs(Session session) {
        return session.createCriteria(JobRecord.class).add(Restrictions.ne("state", JobState.COMPLETED)).list();
    }

}
