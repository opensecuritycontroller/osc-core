package org.osc.core.broker.service.response;


public class CheckNetworkSettingResponse implements Response {

    private boolean hasDeployedInstances;

    public boolean hasDeployedInstances() {
        return this.hasDeployedInstances;
    }

    public void setHasDeployedInstances(boolean isSuccess) {
        this.hasDeployedInstances = isSuccess;
    }

}
