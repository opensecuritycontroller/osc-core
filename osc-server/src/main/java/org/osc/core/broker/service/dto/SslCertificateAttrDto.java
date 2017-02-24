package org.osc.core.broker.service.dto;

import io.swagger.annotations.ApiModelProperty;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

// SSL Certificate attributes Data Transfer Object associated with VC entity
@XmlRootElement(name = "sslCertificateAttributes")
@XmlAccessorType(XmlAccessType.FIELD)
public class SslCertificateAttrDto extends BaseDto {

    @ApiModelProperty(value = "SSL certificate alias")
    private String alias = "";

    @ApiModelProperty(value = "SHA1 fingerprint of the certificate")
    private String sha1 = "";

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    @Override
    public String toString() {
        return "SslCertificateAttrDto{" +
                "alias='" + alias + '\'' +
                ", sha1='" + sha1 + '\'' +
                '}';
    }
}
