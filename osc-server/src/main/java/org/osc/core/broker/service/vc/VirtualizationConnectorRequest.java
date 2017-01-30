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

    VirtualizationConnectorRequest() {
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
