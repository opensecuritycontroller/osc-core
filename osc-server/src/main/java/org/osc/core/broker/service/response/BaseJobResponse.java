package org.osc.core.broker.service.response;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import io.swagger.annotations.ApiModelProperty;

/**
 * A basic Response object which contains a job id in addition to any properties inherited from
 * {@link BaseResponse}
 */

@XmlRootElement(name ="jobResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class BaseJobResponse extends BaseResponse {

    @ApiModelProperty(value = "Id of the Job started", readOnly = true)
    private Long jobId;

    public BaseJobResponse() {
    }

    public BaseJobResponse(Long jobId) {
        this.jobId = jobId;
    }

    public BaseJobResponse(Long objectId, Long jobId) {
        super(objectId);
        this.jobId = jobId;
    }

    public Long getJobId() {
        return this.jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }
}
