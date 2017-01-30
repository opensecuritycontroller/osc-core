package org.osc.core.rest.client.agent.model.input;

import java.net.URL;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentUpgradeRequest {
    private URL upgradePackageUrl;

    public URL getUpgradePackageUrl() {
        return upgradePackageUrl;
    }

    public void setUpgradePackageUrl(URL upgradePackageUrl) {
        this.upgradePackageUrl = upgradePackageUrl;
    }

    @Override
    public String toString() {
        return "AgentUpgradeRequest [upgradePackageUrl=" + upgradePackageUrl + "]";
    }

}
