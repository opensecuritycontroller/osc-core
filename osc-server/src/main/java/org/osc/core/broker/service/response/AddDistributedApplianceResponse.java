package org.osc.core.broker.service.response;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.service.dto.DistributedApplianceDto;

@XmlRootElement(name = "distributedApplianceResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class AddDistributedApplianceResponse extends DistributedApplianceDto implements Response {

    private Long jobId;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

}
