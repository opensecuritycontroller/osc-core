package org.osc.core.rest.client.crypto.model;

import java.security.cert.X509Certificate;
import java.util.Date;

public class CertificateBasicInfoModel {

    private final String alias;
    private final String sha1Fingerprint;
    private final Date validFrom;
    private final Date validTo;
    private final String algorithmType;
    private final X509Certificate certificate;

    public CertificateBasicInfoModel(String alias) {
        this.alias = alias;
        this.certificate = null;
        this.algorithmType = "-";
        this.validFrom = null;
        this.validTo = null;
        this.sha1Fingerprint = null;
    }

    public CertificateBasicInfoModel(String alias, String sha1Fingerprint, Date validFrom, Date validTo, String algorithmType, X509Certificate certificate) {
        this.alias = alias;
        this.sha1Fingerprint = sha1Fingerprint;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.algorithmType = algorithmType;
        this.certificate = certificate;
    }

    public String getAlias() {
        return alias;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public Date getValidTo() {
        return validTo;
    }

    public String getAlgorithmType() {
        return algorithmType;
    }

    public String getSha1Fingerprint() {
        return sha1Fingerprint;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }
}