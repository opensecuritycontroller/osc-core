package org.osc.core.rest.client.agent.model.output.dpaipc;

public class StatusResponse {
    public int status;
    public String error;
    public Object response;

    @Override
    public String toString() {
        return "StatusResponse [status=" + status + ", error=" + error + ", response=" + response + "]";
    }

}
