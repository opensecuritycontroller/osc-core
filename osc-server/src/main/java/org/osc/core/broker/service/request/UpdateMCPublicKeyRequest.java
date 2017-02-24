/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
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
