package org.osc.core.rest.client.crypto.model;

import org.apache.log4j.Logger;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;

@XmlRootElement(name = "certificateBasicInfoModel")
@XmlAccessorType(XmlAccessType.FIELD)
public class CertificateBasicInfoModel {

    private static final Logger LOG = Logger.getLogger(CertificateBasicInfoModel.class);

    private String alias;
    private String sha1Fingerprint;
    private Date validFrom;
    private Date validTo;
    private String algorithmType;

    private X509Certificate certificate;

    private String certificateContent;

    private boolean isConnected;

    public CertificateBasicInfoModel() {
    }

    public CertificateBasicInfoModel(String alias) {
        this.alias = alias;
        this.certificate = null;
        this.algorithmType = "-";
        this.validFrom = null;
        this.validTo = null;
        this.sha1Fingerprint = null;
        this.certificateContent = "";
    }

    public CertificateBasicInfoModel(String alias, String sha1Fingerprint, Date validFrom, Date validTo, String algorithmType, X509Certificate certificate) {
        this.alias = alias;
        this.sha1Fingerprint = sha1Fingerprint;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.algorithmType = algorithmType;
        this.certificate = certificate;
        this.certificateContent = certificateToString(certificate);
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getSha1Fingerprint() {
        return sha1Fingerprint;
    }

    public void setSha1Fingerprint(String sha1Fingerprint) {
        this.sha1Fingerprint = sha1Fingerprint;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public String getAlgorithmType() {
        return algorithmType;
    }

    public void setAlgorithmType(String algorithmType) {
        this.algorithmType = algorithmType;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public String getCertificateContent() {
        return this.certificateContent;
    }

    public void setCertificateContent(String certificateContent) {
        this.certificateContent = certificateContent;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    private String certificateToString(X509Certificate certificate) {
        try {
            StringBuilder cert = new StringBuilder();
            cert.append("-----BEGIN CERTIFICATE----- ");
            cert.append(DatatypeConverter.printBase64Binary(certificate.getEncoded()));
            cert.append(" -----END CERTIFICATE-----");
            return cert.toString();
        } catch (CertificateEncodingException e) {
            LOG.error("Cannot encode certificate", e);
            return "";
        }
    }
}