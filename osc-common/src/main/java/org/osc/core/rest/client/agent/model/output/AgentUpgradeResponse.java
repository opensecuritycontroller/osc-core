package org.osc.core.rest.client.agent.model.output;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentUpgradeResponse {

    private String upgradeStatus;

    public String getUpgradeStatus() {
        return upgradeStatus;
    }

    public void setUpgradeStatus(String upgradeStatus) {
        this.upgradeStatus = upgradeStatus;
    }

    @Override
    public String toString() {
        return "AgentUpgradeResponse [upgradeStatus=" + upgradeStatus + "]";
    }

}
