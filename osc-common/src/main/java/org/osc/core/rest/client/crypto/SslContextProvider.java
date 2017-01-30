package org.osc.core.rest.client.crypto;

import org.apache.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.SecureRandom;

public class SslContextProvider {

    private final Logger log = Logger.getLogger(SslContextProvider.class);

    public SSLContext getSSLContext() {
        SSLContext ctx = null;
        try {
            TrustManager[] trustManager = new TrustManager[]{X509TrustManagerFactory.getInstance()};
            ctx = SSLContext.getInstance("TLSv1.2");
            ctx.init(null, trustManager, new SecureRandom());
        } catch (Exception e) {
            log.error("Encountering security exception", e);
        }
        return ctx;
    }
}