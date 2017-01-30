package org.osc.core.broker.rest.client.nsx.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceInstances {

    @XmlElement(name = "serviceInstance")
    public List<ServiceInstance> list = new ArrayList<ServiceInstance>();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (ServiceInstance si : this.list) {
            sb.append(si.toString() + "\n");
        }
        return sb.toString();
    }

    public ServiceInstance findServiceInstanceByService(Service sv) {
        for (ServiceInstance si : this.list) {
            if (si.getService().objectId.equals(sv.getId())) {
                return si;
            }
        }
        return null;
    }
}
