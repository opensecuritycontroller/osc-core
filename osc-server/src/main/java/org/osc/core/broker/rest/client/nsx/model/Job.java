package org.osc.core.broker.rest.client.nsx.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Job {
    public String id;
    public String name;
    public String description;

    @Override
    public String toString() {
        return "Job [id=" + id + ", name=" + name + ", description=" + description + "]";
    }
}
