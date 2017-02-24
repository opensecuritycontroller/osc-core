/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
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