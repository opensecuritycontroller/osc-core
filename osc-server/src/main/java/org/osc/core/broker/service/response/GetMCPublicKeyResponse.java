package org.osc.core.broker.service.response;

import org.osc.core.rest.client.annotations.VmidcLogHidden;

public class GetMCPublicKeyResponse implements Response {

    @VmidcLogHidden
    private byte[] publicKey = null;

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

}
