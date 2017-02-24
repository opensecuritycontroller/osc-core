package org.osc.core.rest.client.agent.model.input;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings("serial")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class EndpointGroupList implements Serializable {

    @XmlElement
    public List<EndpointGroup> endpointGroups = new ArrayList<EndpointGroup>();

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class EndpointGroup implements Serializable{
        public String id;
        public String name;
        public String type; // IP or MAC
        public List<String> addresses = new ArrayList<String>();

        @Override
        public String toString() {
            return "Endpoints [id=" + id + ", name=" + name + ", type=" + type + ", address=" + addresses + "]";
        }

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (EndpointGroup container : endpointGroups) {
            sb.append(container.toString() + "\n");
        }
        return sb.toString();
    }

}
