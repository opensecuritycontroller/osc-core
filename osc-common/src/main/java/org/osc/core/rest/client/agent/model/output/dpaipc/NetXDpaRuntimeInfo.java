package org.osc.core.rest.client.agent.model.output.dpaipc;

import com.google.gson.annotations.SerializedName;

public class NetXDpaRuntimeInfo extends StatusResponse {
    @SerializedName("last-update")
    public String lastUpdate;
    @SerializedName("current-update")
    public String currentUpdate;
    @Override
    public String toString() {
        return "NetXDpaRuntimeInfo [lastUpdate=" + lastUpdate + ", currentUpdate=" + currentUpdate + "]";
    }
}