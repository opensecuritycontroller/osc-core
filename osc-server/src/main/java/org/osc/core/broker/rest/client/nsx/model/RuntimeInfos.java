package org.osc.core.broker.rest.client.nsx.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class RuntimeInfos {
    protected List<ServiceInstanceRuntimeInfo> serviceInstanceRuntimeInfo;

    public List<ServiceInstanceRuntimeInfo> getServiceInstanceRuntimeInfo() {
        return serviceInstanceRuntimeInfo;
    }

    public void setServiceInstanceRuntimeInfo(List<ServiceInstanceRuntimeInfo> value) {
        this.serviceInstanceRuntimeInfo = value;
    }
}
