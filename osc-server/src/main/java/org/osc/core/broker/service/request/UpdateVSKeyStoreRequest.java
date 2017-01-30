package org.osc.core.broker.service.request;

import org.osc.core.rest.client.annotations.VmidcLogHidden;


public class UpdateVSKeyStoreRequest implements Request {

    private Long vsId;
    @VmidcLogHidden
    private byte[] keyStore = null;

    public Long getVsId() {
        return vsId;
    }

    public void setVsId(Long vsId) {
        this.vsId = vsId;
    }

    public byte[] getKeyStore() {
        return keyStore;
    }

    public void setSigfile(byte[] keyStore) {
        this.keyStore = keyStore;
    }

    @Override
    public String toString() {
        return "UpdateVSKeyStoreRequest [vsId=" + vsId + "]";
    }

}
