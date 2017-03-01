package org.osc.core.rest.client.crypto;

import org.apache.log4j.Logger;
import org.osc.core.rest.client.crypto.model.CertificateResolverModel;
import org.osc.core.rest.client.exception.SslCertificateResolverException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
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
     * @throws SslCertificateResolverException exception handles potential SSL error causes
     * @throws IOException
     */
    public void fetchCertificatesFromURL(URL url, String aliasPrefix)
            throws SslCertificateResolverException, IOException {

        SSLContext sslCtx = new SslContextProvider().getAcceptAllSSLContext();
        HttpsURLConnection urlConnection;
        urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setHostnameVerifier((string, ssls) -> true);

        urlConnection.setSSLSocketFactory(sslCtx.getSocketFactory());
        urlConnection.connect();

        try {
            LOG.debug("Successfully connected to: " + url.toString());
            Certificate[] serverCertificates = urlConnection.getServerCertificates();
            for (Certificate serverCertificate : serverCertificates) {
                X509Certificate certificate = ((X509Certificate) serverCertificate);
                certificate.checkValidity();
                long unixTimestamp = Instant.now().getEpochSecond();
                CertificateResolverModel model = new CertificateResolverModel(certificate, aliasPrefix + "_" + unixTimestamp,
                        X509TrustManagerFactory.getSha1Fingerprint(certificate));
                LOG.debug("Found certificate with fingerprint: " + model.getSha1());
                this.certificateResolverModels.add(model);
            }
        } catch (CertificateEncodingException | NoSuchAlgorithmException | SSLPeerUnverifiedException e) {
            throw new SslCertificateResolverException("Failed to fetch certificate: " + e.getMessage(), e);
        } catch (CertificateNotYetValidException e) {
            throw new SslCertificateResolverException("Failed to fetch not yet valid certificate: " + e.getMessage(), e);
        } catch (CertificateExpiredException e) {
            throw new SslCertificateResolverException("Failed to fetch expired certificate: " + e.getMessage(), e);
        } finally {
            urlConnection.disconnect();
        }
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