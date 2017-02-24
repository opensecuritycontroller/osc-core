package org.osc.core.broker.service.response;


public class SyncApplianceManagerConnectorResponse implements Response {
    private Long jobId;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    @Override
    public String toString() {
        return "SyncApplianceManagerConnectorResponse [jobId=" + jobId + "]";
    }

}
