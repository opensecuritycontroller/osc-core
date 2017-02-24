package org.osc.core.broker.service.request;

public class ListTaskRequest implements Request {
    private long jobId;

    public long getJobId() {
        return jobId;
    }

    public void setJobId(long jobId) {
        this.jobId = jobId;
    }

}
