package org.osc.core.broker.service.transactions;

/**
 * Input container for CompleteJobTransaction logic
 */
public class CompleteJobTransactionInput {
    private long entityId;
    private long jobId;

    public CompleteJobTransactionInput(long entityId, long jobId) {
        this.entityId = entityId;
        this.jobId = jobId;
    }

    public long getEntityId() {
        return this.entityId;
    }

    public long getJobId() {
        return this.jobId;
    }
}
