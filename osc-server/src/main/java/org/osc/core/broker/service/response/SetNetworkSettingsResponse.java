package org.osc.core.broker.service.response;

public class SetNetworkSettingsResponse implements Response {

    Long jobId;

    /**
     * @return the jobId
     */
    public Long getJobId() {
        return jobId;
    }

    /**
     * @param jobId
     *            the jobId to set
     */
    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

}
