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