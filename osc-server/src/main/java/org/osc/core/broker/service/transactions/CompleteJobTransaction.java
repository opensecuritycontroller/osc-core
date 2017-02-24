package org.osc.core.broker.service.transactions;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.model.entities.job.LastJobContainer;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.util.db.TransactionalRunner;

/**
 * Attaches (in transaction) the last job record to given entity
 * @param <T> type that implements LastJobContainer (IscEntity with ability to attach last job)
 */
public class CompleteJobTransaction<T extends LastJobContainer> implements TransactionalRunner.TransactionalAction<Void, CompleteJobTransactionInput> {
    private Class<T> entityType;

    public CompleteJobTransaction(Class<T> entityType) {
        this.entityType = entityType;
    }

    @Override
    public Void run(Session session, CompleteJobTransactionInput param) throws Exception {
        @SuppressWarnings("unchecked")
        T entity = (T) session.get(this.entityType, param.getEntityId());

        if (entity == null) {
            throw new IllegalArgumentException(String.format("Entity with Id '%d' does not exist.", param.getEntityId()));
        }

        JobRecord jobRecord = (JobRecord) session.get(JobRecord.class, param.getJobId());

        if (jobRecord == null) {
            throw new IllegalArgumentException(String.format("Job record with id '%d' does not exist.", param.getJobId()));
        }

        entity.setLastJob((JobRecord) session.get(JobRecord.class, param.getJobId()));
        EntityManager.update(session, entity);

        return null;
    }
}