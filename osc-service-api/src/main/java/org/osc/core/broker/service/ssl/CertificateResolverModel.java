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
package org.osc.core.broker.service.ssl;

import java.security.cert.X509Certificate;

/**
 * Support class for fetched certificates
 */
public class CertificateResolverModel {

    private final X509Certificate certificate;
    private String alias;
    private final String sha1;

    /**
     * Constructor for fetched certificate
     * @param certificate - certificate content
     * @param sha1 - fingerprint of certificate
     */
    public CertificateResolverModel(X509Certificate certificate, String sha1) {
        this.certificate = certificate;
        this.sha1 = sha1;
    }

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
        return this.certificate;
    }

    /**
     * @return alias prefix
     */
    public String getAlias() {
        return this.alias;
    }

    /**
     * @return SHA1 fingerprint of certificate
     */
    public String getSha1() {
        return this.sha1;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
}