package org.osc.core.broker.service.request;

import org.osc.core.rest.client.annotations.VmidcLogHidden;


public class UpdateMCPublicKeyRequest implements Request {

    private Long mcId;
    @VmidcLogHidden
    private byte[] publicKey = null;

    public Long getMcId() {
        return mcId;
    }

    public void setMcIp(Long mcId) {
        this.mcId = mcId;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public String toString() {
        return "UpdateMCPublicKeyRequest [mcId=" + mcId + "]";
    }
}
