package org.osc.core.broker.rest.client.nsx.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.osc.sdk.sdn.element.DeploymentSpecElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class VersionedDeploymentSpec implements DeploymentSpecElement {
    private String hostVersion;
    private final boolean vmciEnabled;
    private String ovfUrl;
    private String id;
    private String revision;

    public VersionedDeploymentSpec() {
        this.vmciEnabled = true;
    }

    public VersionedDeploymentSpec(DeploymentSpecElement deploymentSpec) {
        this.id = deploymentSpec.getId();
        this.ovfUrl = deploymentSpec.getImageUrl();
        this.hostVersion = deploymentSpec.getHostVersion();
        this.vmciEnabled = true;
        this.revision = deploymentSpec.getRevision();
    }

    public void setHostVersion(String hostVersion) {
        this.hostVersion = hostVersion;
    }

    @Override
    public String getHostVersion() {
        return this.hostVersion;
    }

    private boolean isVmciEnabled() {
        return this.vmciEnabled;
    }

    @Override
    public String getImageUrl() {
        return this.ovfUrl;
    }

    public void setOvfUrl(String ovfUrl) {
        this.ovfUrl = ovfUrl;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getRevision() {
        return this.revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (getClass() != object.getClass()) {
            return false;
        }
        if (this == object) {
            return true;
        }

        VersionedDeploymentSpec other = (VersionedDeploymentSpec) object;

        return new EqualsBuilder()
                .append(getHostVersion(), other.getHostVersion())
                .append(getImageUrl(), other.getImageUrl())
                .append(getId(), other.getId())
                .append(getRevision(), other.getRevision())
                .append(isVmciEnabled(), other.isVmciEnabled())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getHostVersion())
                .append(getImageUrl())
                .append(getId())
                .append(getRevision())
                .append(isVmciEnabled())
                .toHashCode();
    }

    @Override
    public String toString() {
        return String.format("%s [Id: %s, HostVerion: %s, ovfUrl: %s]", this.getClass().getSimpleName(), this.id,
                this.hostVersion, this.ovfUrl);
    }
}
