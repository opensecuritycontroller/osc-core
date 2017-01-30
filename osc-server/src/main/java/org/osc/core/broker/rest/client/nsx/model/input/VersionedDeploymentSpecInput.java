package org.osc.core.broker.rest.client.nsx.model.input;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "versionedDeploymentSpec")
@XmlAccessorType(XmlAccessType.FIELD)
public class VersionedDeploymentSpecInput {
    private String hostVersion;
    private boolean vmciEnabled = true;
    private String ovfUrl = "";

    public String getHostVersion() {
        return hostVersion;
    }

    public void setHostVersion(String hostVersion) {
        this.hostVersion = hostVersion;
    }

    public boolean isVmciEnabled() {
        return vmciEnabled;
    }

    public void setVmciEnabled(boolean isEnabled) {
        this.vmciEnabled = isEnabled;
    }

    public String getOvfUrl() {
        return ovfUrl;
    }

    public void setOvfUrl(String ovfUrl) {
        this.ovfUrl = ovfUrl;
    }
}
