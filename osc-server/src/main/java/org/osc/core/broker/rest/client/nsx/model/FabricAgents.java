package org.osc.core.broker.rest.client.nsx.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement()
@XmlAccessorType(XmlAccessType.FIELD)
public class FabricAgents {

    @XmlElement(name = "agent")
    public List<Agent> list;

    @Override
    public String toString() {
        if (list == null) {
            return super.toString();
        }

        StringBuilder sb = new StringBuilder();
        for (Agent si : list) {
            sb.append(si.toString() + "\n");
        }
        return sb.toString();
    }
}
