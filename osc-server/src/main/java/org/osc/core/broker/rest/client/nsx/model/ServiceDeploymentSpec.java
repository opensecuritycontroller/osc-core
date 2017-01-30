package org.osc.core.broker.rest.client.nsx.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceDeploymentSpec {

    private String id;
    private String name;
    private ServiceDeploymentVersionedSpecs versionedSpecs;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ServiceDeploymentVersionedSpecs getVersionedSpecs() {
        return versionedSpecs;
    }

    public void setVersionedSpecs(ServiceDeploymentVersionedSpecs versionedSpecs) {
        this.versionedSpecs = versionedSpecs;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(versionedSpecs.toString() + "\n");
        return sb.toString();
    }
}
