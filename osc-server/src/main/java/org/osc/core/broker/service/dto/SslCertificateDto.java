package org.osc.core.broker.service.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "sslCertificateDto")
@XmlAccessorType(XmlAccessType.FIELD)
public class SslCertificateDto {

    private String alias;
    private String certificate;

    public SslCertificateDto() {
    }

    public SslCertificateDto(String alias, String certificate) {
        this.certificate = certificate;
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }
}