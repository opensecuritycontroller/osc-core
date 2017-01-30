package org.osc.core.rest.client.crypto;

import org.apache.log4j.Logger;
import org.osc.core.rest.client.crypto.model.CertificateResolverModel;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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

    private SSLContext getLocalSSLContext() {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1) throws java.security.cert.CertificateException {}

            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1) throws java.security.cert.CertificateException {}
        } };

        SSLContext ctx = null;

        try {
            ctx = SSLContext.getInstance("TLSv1.2");
            ctx.init(null, trustAllCerts, new SecureRandom());
        } catch (java.security.GeneralSecurityException ex) {
            LOG.error("Encountering security exception", ex);
        }

        return ctx;
    }

    /**
     * Fetches certificates from specified URL using local SSL context
     * @param url endpoint address
     * @param aliasPrefix prefix for alias i.e. vmware, openstack, manager
     * @throws IOException
     */
    public void fetchCertificatesFromURL(URL url, String aliasPrefix) throws IOException {
        SSLContext sslCtx = getLocalSSLContext();
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setHostnameVerifier((string, ssls) -> true);

        urlConnection.setSSLSocketFactory(sslCtx.getSocketFactory());
        urlConnection.connect();

        Stream<Certificate> certificateStream = Arrays.stream(urlConnection.getServerCertificates());
        certificateStream.forEach(cert -> {
            if(cert instanceof X509Certificate) {
                try {
                    X509Certificate certificate = ((X509Certificate) cert);
                    certificate.checkValidity();
                    long unixTimestamp = Instant.now().getEpochSecond();
                    CertificateResolverModel model = new CertificateResolverModel(certificate, aliasPrefix + "_" + unixTimestamp,
                            X509TrustManagerFactory.getSha1Fingerprint(certificate));
                    this.certificateResolverModels.add(model);
                } catch(CertificateExpiredException cee) {
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
     * Returns list of fetched certificates necessary to save in database and truststore
     * @return certificates list
     */
    public ArrayList<CertificateResolverModel> getCertificateResolverModels() {
        return this.certificateResolverModels;
    }
}