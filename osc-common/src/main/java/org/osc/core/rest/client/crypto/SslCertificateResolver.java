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
import org.osc.core.rest.client.crypto.model.CertificateResolverModel;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Class handles fetching SSL certificates from external source before the connection with specified endpoint could be established
 */
public class SslCertificateResolver {

    private static final Logger LOG = Logger.getLogger(SslCertificateResolver.class);

    private ArrayList<CertificateResolverModel> certificateResolverModels = new ArrayList<>();

    /**
     * Fetches certificates from specified URL using local SSL context
     *
     * @param url         endpoint address
     * @param aliasPrefix prefix for alias i.e. vmware, openstack, manager
     * @throws IOException
     */
    public void fetchCertificatesFromURL(URL url, String aliasPrefix) throws IOException {
        SSLContext sslCtx = new SslContextProvider().getAcceptAllSSLContext();
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setHostnameVerifier((string, ssls) -> true);

        urlConnection.setSSLSocketFactory(sslCtx.getSocketFactory());
        urlConnection.connect();

        Stream<Certificate> certificateStream = Arrays.stream(urlConnection.getServerCertificates());
        certificateStream.forEach(cert -> {
            if (cert instanceof X509Certificate) {
                try {
                    X509Certificate certificate = ((X509Certificate) cert);
                    certificate.checkValidity();
                    long unixTimestamp = Instant.now().getEpochSecond();
                    CertificateResolverModel model = new CertificateResolverModel(certificate, aliasPrefix + "_" + unixTimestamp,
                            X509TrustManagerFactory.getSha1Fingerprint(certificate));
                    this.certificateResolverModels.add(model);
                } catch (CertificateExpiredException cee) {
                    LOG.error("Improper certificate: certificate expired", cee);
                } catch (CertificateNotYetValidException e) {
                    LOG.error("Improper certificate: certificate not yet validated", e);
                } catch (CertificateEncodingException | NoSuchAlgorithmException e) {
                    LOG.error("Cannot generate sha1 fingerprint for certificate", e);
                }
            } else {
                LOG.error("Improper certificate: unknown certificate type: " + cert);
            }
        });

        urlConnection.disconnect();
    }

    /**
     * Checks if given throwable is instance of SSLException
     *
     * @param originalCause - error cause
     * @return bool - verified cause status
     */
    public boolean checkExceptionTypeForSSL(Throwable originalCause) {
        Throwable cause = originalCause;
        while (null != cause.getCause()) {
            cause = cause.getCause();
            if (cause instanceof SSLException) {
                return true;
            }
        }

        String detailedMessage = originalCause.getMessage();
        return detailedMessage != null && detailedMessage.contains("javax.net.ssl.SSL");
    }

    /**
     * Returns list of fetched certificates necessary to save in database and truststore
     *
     * @return certificates list
     */
    public ArrayList<CertificateResolverModel> getCertificateResolverModels() {
        return this.certificateResolverModels;
    }
}