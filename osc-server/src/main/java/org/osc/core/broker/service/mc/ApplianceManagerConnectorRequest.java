package org.osc.core.broker.service.mc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.service.dto.ApplianceManagerConnectorDto;
import org.osc.core.broker.service.request.Request;

// Appliance Manager Connector Data Transfer Object associated with MC Entity
@XmlRootElement(name ="applianceManagerConnectorRequest")
@XmlAccessorType(XmlAccessType.FIELD)
public class ApplianceManagerConnectorRequest extends ApplianceManagerConnectorDto implements Request {

    private boolean skipRemoteValidation;
    private boolean forceAddSSLCertificates;

    ApplianceManagerConnectorRequest() {
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
        return "ApplianceManagerConnectorRequest{" +
                "skipRemoteValidation=" + skipRemoteValidation +
                ", forceAddSSLCertificates=" + forceAddSSLCertificates +
                '}';
    }
}
