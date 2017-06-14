/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
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
package org.osc.core.broker.util.crypto;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.log4j.Logger;

public final class SslContextProvider {

    private static final Logger LOG = Logger.getLogger(SslContextProvider.class);

    private SSLContext sslContext;

    private SslContextProvider() {
        // load SSL context
        TrustManager[] trustManager = new TrustManager[]{X509TrustManagerFactory.getInstance()};
        try {
            this.sslContext = SSLContext.getInstance("TLSv1.2");
            this.sslContext.init(null, trustManager, new SecureRandom());

            // disable SSL session caching - we load SSL certificates dinamically so we need to ensure
            // that we have up to date cached certificates list
            this.sslContext.getClientSessionContext().setSessionCacheSize(1);
            this.sslContext.getClientSessionContext().setSessionTimeout(1);
            this.sslContext.getServerSessionContext().setSessionCacheSize(1);
            this.sslContext.getServerSessionContext().setSessionTimeout(1);

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            LOG.error("Encountering security exception in SSL context", e);
            throw new RuntimeException("Internal error with SSL context", e);
        }
    }

    private static volatile SslContextProvider instance;

    public static SslContextProvider getInstance() {
        if (instance == null) {
            synchronized(SslContextProvider.class) {
                if(instance == null) {
                    instance = new SslContextProvider();
                }
            }
        }

        return instance;
    }

    /**
     * Provides SSL context which accepts SSL connections with trust store verification
     *
     * @return SSLContext
     */
    public SSLContext getSSLContext() {
        return this.sslContext;
    }
}