package org.osc.core.rest.client.crypto;

/**
 * Support class for SSL handling used by X509TrustManagerFactory
 */
class SslConfig {

    private static final String TYPE_JKS = "JKS";

    private final String truststorefile;
    private final String truststorepass;
    private final String truststoretype;

    /**
     * Constructor for SSL configuration with predefined type: JKS
     * @param truststorefile - path to trust store file
     * @param truststorepass - password for trust store
     */
    SslConfig(String truststorefile, String truststorepass) {
        this.truststorefile = truststorefile;
        this.truststorepass = truststorepass;
        this.truststoretype = TYPE_JKS;
    }

    String getTruststorefile() {
        return truststorefile;
    }

    String getTruststorepass() {
        return truststorepass;
    }

    String getTruststoretype() {
        return truststoretype;
    }
}