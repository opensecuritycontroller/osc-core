package org.osc.core.broker.service.request;


public class GetVSPublicKeyRequest implements Request {

    private Long vsId;

    public Long getVsId() {
        return vsId;
    }

    public void setVsId(Long vsId) {
        this.vsId = vsId;
    }

}
