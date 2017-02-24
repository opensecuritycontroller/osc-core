package org.osc.core.broker.rest.client.nsx.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceInstanceRuntimeInfo {
    protected Long id;
    protected Long revision;
    protected String status;
    protected String installState;
    protected DeploymentScope deploymentScope;

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public Long getRevision() {
        return revision;
    }

    public void setRevision(Long value) {
        this.revision = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        this.status = value;
    }

    public String getInstallState() {
        return installState;
    }

    public void setInstallState(String value) {
        this.installState = value;
    }

    public DeploymentScope getDeloymentScope() {
        return deploymentScope;
    }

    public void setDeloymentScope(DeploymentScope value) {
        this.deploymentScope = value;
    }

}
