package org.osc.core.broker.rest.client.nsx.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement()
@XmlAccessorType(XmlAccessType.FIELD)
public class DeployedServices {

    @XmlElement(name = "deployedService")
    private List<DeployedService> list;

    public List<DeployedService> getList() {
        return list;
    }

    public void setList(List<DeployedService> list) {
        this.list = list;
    }

    @Override
    public String toString() {
        if (list == null) {
            return super.toString();
        }

        StringBuilder sb = new StringBuilder();
        for (DeployedService ds : list) {
            sb.append(ds.toString() + "\n");
        }
        return sb.toString();
    }
}
