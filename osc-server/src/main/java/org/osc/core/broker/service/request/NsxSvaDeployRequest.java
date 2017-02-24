package org.osc.core.broker.service.request;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.service.dto.BaseDto;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class NsxSvaDeployRequest extends BaseRequest<BaseDto> {
    public Long vsId;
    public String clusterName;
    public String datastoreName;
    public String svaPortGroupName;
    public String ipPoolName;
}
