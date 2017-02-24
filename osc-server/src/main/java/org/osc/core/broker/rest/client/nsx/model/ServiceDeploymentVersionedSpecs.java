package org.osc.core.broker.rest.client.nsx.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceDeploymentVersionedSpecs {

    private List<VersionedDeploymentSpec> versionedDeploymentSpec;

    public void setVersionedDeploymentSpec(List<VersionedDeploymentSpec> versionedDeploymentSpec) {
        this.versionedDeploymentSpec = versionedDeploymentSpec;
    }

    public List<VersionedDeploymentSpec> getVersionedDeploymentSpec() {
        return versionedDeploymentSpec;
    }

    @Override
    public String toString() {
        if (versionedDeploymentSpec == null) {
            return super.toString();
        }

        StringBuilder sb = new StringBuilder();
        for (VersionedDeploymentSpec ds : versionedDeploymentSpec) {
            sb.append(ds.toString() + "\n");
        }
        return sb.toString();
    }
}
