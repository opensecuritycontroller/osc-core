package org.osc.core.rest.client.crypto.model;

import java.security.cert.X509Certificate;

/**
 * Support class for fetched certificates
 */
public class CertificateResolverModel {

    private final X509Certificate certificate;
    private final String alias;
    private final String sha1;

    /**
     * Constructor for fetched certificate
     * @param certificate - certificate content
     * @param alias - alias prefix for certificate
     * @param sha1 - fingerprint of certificate
     */
    public CertificateResolverModel(X509Certificate certificate, String alias, String sha1) {
        this.certificate = certificate;
        this.alias = alias;
        this.sha1 = sha1;
    }

    /**
     * @return X509Certificate certificate
     */
    public X509Certificate getCertificate() {
        return certificate;
    }

    /**
     * @return alias prefix
     */
    public String getAlias() {
        return alias;
    }

    /**
     * @return SHA1 fingerprint of certificate
     */
    public String getSha1() {
        return sha1;
    }
}