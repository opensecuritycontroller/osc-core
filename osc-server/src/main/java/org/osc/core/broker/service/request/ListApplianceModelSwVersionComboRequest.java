package org.osc.core.broker.service.request;

import org.osc.core.broker.model.plugin.manager.ManagerType;



public class ListApplianceModelSwVersionComboRequest implements Request {
    private ManagerType type;

    public ManagerType getType() {
        return type;
    }

    public void setType(ManagerType type) {
        this.type = type;
    }

}
