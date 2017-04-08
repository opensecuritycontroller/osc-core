/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
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
package org.osc.core.broker.service.vc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.request.Request;

@XmlRootElement(name = "virtualizationConnectorRequest")
@XmlAccessorType(XmlAccessType.FIELD)
public class VirtualizationConnectorRequest extends VirtualizationConnectorDto implements Request {

    private boolean skipRemoteValidation;
    private boolean forceAddSSLCertificates;

    public VirtualizationConnectorRequest() {
    }

    public boolean isSkipRemoteValidation() {
        return this.skipRemoteValidation;
    }

    public void setSkipRemoteValidation(boolean skipRemoteValidation) {
        this.skipRemoteValidation = skipRemoteValidation;
    }

    public boolean isForceAddSSLCertificates() {
        return forceAddSSLCertificates;
    }

    public void setForceAddSSLCertificates(boolean forceAddSSLCertificates) {
        this.forceAddSSLCertificates = forceAddSSLCertificates;
    }

    @Override
    public String toString() {
        return "VirtualizationConnectorRequest{" +
                "skipRemoteValidation=" + skipRemoteValidation +
                ", forceAddSSLCertificates=" + forceAddSSLCertificates +
                '}';
    }
}
