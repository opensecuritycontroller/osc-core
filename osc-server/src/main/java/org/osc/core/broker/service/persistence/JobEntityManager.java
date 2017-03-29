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

import static org.osc.core.broker.model.entities.job.JobState.COMPLETED;
import static org.osc.core.broker.model.entities.job.JobStatus.FAILED;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.osc.core.broker.job.JobState;
import org.osc.core.broker.job.JobStatus;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockObjectReference.ObjectType;
import org.osc.core.broker.model.entities.job.JobObject;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.service.dto.JobRecordDto;
import org.osc.core.broker.service.exceptions.VmidcException;
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

    public static Long getTaskCount(Long jobId) throws InterruptedException, VmidcException {

        EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();

        try {
            EntityTransaction tx = em.getTransaction();
            tx.begin();

            String hql = "SELECT count(*) FROM TaskRecord WHERE job_fk = :jobId";

            TypedQuery<Long> query = em.createQuery(hql, Long.class);
            query.setParameter("jobId", jobId);
            Long count = query.getSingleResult();

            tx.commit();

            return count;

        } finally {

            if (em != null) {
                em.close();
            }
        }

    }

    public static Long getCompletedTaskCount(Long jobId) throws InterruptedException, VmidcException {

        EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();

        try {
            EntityTransaction tx = em.getTransaction();
            tx.begin();

            String hql = "SELECT count(*) FROM TaskRecord WHERE job_fk = :jobId AND state = 'COMPLETED'";

            TypedQuery<Long> query = em.createQuery(hql, Long.class);
            query.setParameter("jobId", jobId);
            Long count = query.getSingleResult();

            tx.commit();

            return count;

        } finally {

            if (em != null) {
                em.close();
            }
        }
    }

    /**
     * Gets the total job count from the database
     * @param session2
     *
     * @return total job count
     */
    public static Long getTotalJobCount(EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> query = cb.createQuery(Long.class);

        query = query.select(cb.count(query.from(JobRecord.class)));
        Long totalJobCount = em.createQuery(query).getSingleResult();

        return totalJobCount;
    }

    /**
     * Gets the total failed job count from the database
     *
     * @return failed job count
     */
    public static Long getTotalJobFailCount(EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> query = cb.createQuery(Long.class);

        Root<JobRecord> from = query.from(JobRecord.class);
        query = query.select(cb.count(from))
                .where(cb.equal(from.get("status"), FAILED));
        Long totalJobFailCount = em.createQuery(query).getSingleResult();

        return totalJobFailCount;
    }

    public List<JobRecord> getUncompletedJobs(EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<JobRecord> query = cb.createQuery(JobRecord.class);

        Root<JobRecord> from = query.from(JobRecord.class);
        query = query.select(from)
                .where(cb.notEqual(from.get("state"), COMPLETED));
        return em.createQuery(query).getResultList();
    }

}
