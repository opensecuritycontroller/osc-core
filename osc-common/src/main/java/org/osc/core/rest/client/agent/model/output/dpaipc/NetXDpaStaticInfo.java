package org.osc.core.rest.client.agent.model.output.dpaipc;


public class NetXDpaStaticInfo extends StatusResponse {
    public String dpaName;
    public String dpaVersion;
    public String ipcVersion;
    @Override
    public String toString() {
        return "NetXDpaStaticInfo [dpaName=" + dpaName + ", dpaVersion=" + dpaVersion + ", ipcVersion=" + ipcVersion
                + "]";
    }
}