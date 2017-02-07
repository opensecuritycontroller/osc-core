package org.osc.core.rest.client.crypto;

import org.apache.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class SslContextProvider {

    private static final Logger LOG = Logger.getLogger(SslContextProvider.class);

    /**
     * Provides SSL context which accepts SSL connections with trust store verification
     *
     * @return SSLContext
     */
    public SSLContext getSSLContext() {
        SSLContext ctx;
        TrustManager[] trustManager;

        try {
            trustManager = new TrustManager[]{X509TrustManagerFactory.getInstance()};
        } catch (Exception e) {
            throw new RuntimeException("Cannot instantiate X509TrustManagerFactory", e);
        }

        try {
            ctx = SSLContext.getInstance("TLSv1.2");
            ctx.init(null, trustManager, new SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            LOG.error("Encountering security exception in SSL context", e);
            throw new RuntimeException("Internal error with SSL context", e);
        }

        return ctx;
    }

    /**
     * Provides SSL context which accepts all HTTPS connections (used to connect with Agent)
     *
     * @return SSLContext
     */
    public SSLContext getAcceptAllSSLContext() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1) throws java.security.cert.CertificateException {
            }

            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1) throws java.security.cert.CertificateException {
            }
        }};

        SSLContext ctx = null;

        try {
            ctx = SSLContext.getInstance("TLSv1.2");
            ctx.init(null, trustAllCerts, new SecureRandom());
        } catch (java.security.GeneralSecurityException ex) {
            LOG.error("Encountering security exception", ex);
        }

        return ctx;
    }
}