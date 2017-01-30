package org.osc.core.broker.service.response;

public class UpdateUserResponse implements Response {

    Long jobId;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

}
