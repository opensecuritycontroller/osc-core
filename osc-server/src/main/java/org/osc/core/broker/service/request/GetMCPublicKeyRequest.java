package org.osc.core.broker.service.request;


public class GetMCPublicKeyRequest implements Request {

    private Long mcId;

    public Long getMcId() {
        return mcId;
    }

    public void setMcId(Long mcId) {
        this.mcId = mcId;
    }

}
