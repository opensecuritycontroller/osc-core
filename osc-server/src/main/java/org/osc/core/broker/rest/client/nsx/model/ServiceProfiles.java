package org.osc.core.broker.rest.client.nsx.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceProfiles {

    @XmlElement(name = "serviceProfile")
    public List<ServiceProfile> list = new ArrayList<ServiceProfile>();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (ServiceProfile sp : this.list) {
            sb.append(sp.toString() + "\n");
        }
        return sb.toString();
    }

    public ServiceProfile findServiceProfileByService(Service sv) {
        for (ServiceProfile sp : this.list) {
            if (sp.getService().getId().equals(sv.getId())) {
                return sp;
            }
        }
        return null;
    }
}
