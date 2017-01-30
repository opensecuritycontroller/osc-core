package org.osc.core.broker.rest.server.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.service.request.Request;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class TagVmRequest implements Request {

    private String applianceInstanceName;
    private String vmUuid;
    private String ipAddress;
    private String tag;

    // TODO prepare UnTagVmRequest with separate validator
    public String getApplianceInstanceName() {
        return applianceInstanceName;
    }

    public void setApplianceInstanceName(String applianceInstanceName) {
        this.applianceInstanceName = applianceInstanceName;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    @Override
    public String toString() {
        return "TagVmRequest [applianceInstanceName=" + applianceInstanceName + ", vmUuid=" + vmUuid
                + ", ipAddress=" + ipAddress + "]";
    }

}
