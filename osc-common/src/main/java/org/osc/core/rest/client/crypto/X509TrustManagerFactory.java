package org.osc.core.rest.client.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.osc.core.rest.client.crypto.model.CertificateBasicInfoModel;

public class X509TrustManagerFactory implements X509TrustManager {

    private static final Logger LOG = Logger.getLogger(X509TrustManagerFactory.class);
    private static X509TrustManagerFactory instance = null;
    private static String TRUSTSTOREFILE = "vmidctruststore.jks";
    private static String TRUSTSTOREPASSWORD = "abc12345";
    private final String ALNUM_FILTER_REGEX = "[^a-zA-Z0-9-_\\.]";
    private X509TrustManager defaultX509TrustManager = null;
    private X509TrustManager rootcaX509TrustManager = null;
    private KeyStore keyStore;
    private SslConfig sslConfig;

    public X509TrustManagerFactory(SslConfig sslConfig) throws Exception {
        this.sslConfig = sslConfig;

        loadDefault();
        try {
            loadCACert(sslConfig);
        } catch (Exception e) {
            LOG.error("Error during loading truststore");
        }
    }

    public static X509TrustManagerFactory getInstance() throws Exception {
        SslConfig sslConfig = new SslConfig(TRUSTSTOREFILE, TRUSTSTOREPASSWORD);
        if (instance == null) {
            instance = new X509TrustManagerFactory(sslConfig);
        } else {
            instance.loadCACert(sslConfig);
        }
        return instance;
    }

    public static String getSha1Fingerprint(X509Certificate cert) throws NoSuchAlgorithmException, CertificateEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] der = cert.getEncoded();
        md.update(der);
        byte[] digest = md.digest();
        return DatatypeConverter.printHexBinary(digest);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        if (this.defaultX509TrustManager != null) {
            return this.defaultX509TrustManager.getAcceptedIssuers();
        } else {
            return new X509Certificate[0];
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        try {
            if (this.defaultX509TrustManager != null) {
                this.defaultX509TrustManager.checkClientTrusted(certs, authType);
            } else {
                throw new CertificateException("No default truststore");
            }
        } catch (CertificateException e) {
            if (this.rootcaX509TrustManager != null) {
                this.rootcaX509TrustManager.checkClientTrusted(certs, authType);
            } else {
                throw e;
            }
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        try {
            if (this.defaultX509TrustManager != null) {
                this.defaultX509TrustManager.checkServerTrusted(certs, authType);
            } else {
                throw new CertificateException("No default truststore");
            }
        } catch (CertificateException e) {
            if (this.rootcaX509TrustManager != null) {
                this.rootcaX509TrustManager.checkServerTrusted(certs, authType);
            } else {
                throw e;
            }
        }
    }

    private void loadDefault() throws Exception {
        this.defaultX509TrustManager = getTrustManager(null);
    }

    private void loadCACert(SslConfig sslConfig) throws Exception {
        String truststorefile = sslConfig.getTruststorefile();

        LOG.info("Opening trust store file....");
        try (InputStream inputStream = new FileInputStream(truststorefile)) {

            this.keyStore = KeyStore.getInstance("JKS");
            String truststoretype = sslConfig.getTruststoretype();

            if ("JKS".equals(truststoretype)) {
                this.keyStore.load(inputStream, sslConfig.getTruststorepass().toCharArray());
            } else {
                this.keyStore.load(null, null);
                CertificateFactory cf = CertificateFactory.getInstance(truststoretype);
                X509Certificate ca = (X509Certificate) cf.generateCertificate(inputStream);
                this.keyStore.setCertificateEntry("ca", ca);
            }

            this.rootcaX509TrustManager = getTrustManager(this.keyStore);

        } catch (FileNotFoundException e) {
            LOG.error("Failed to open trust store file", e);
            throw new Exception("Failed to load trusted certificates", e);
        } catch (CertificateException e) {
            LOG.error("Failed to load certificate from trust store", e);
            throw new Exception("Failed to load certificate from trust store", e);
        }
    }

    public List<CertificateBasicInfoModel> getCertificateInfoList() throws Exception {
        loadCACert(this.sslConfig);
        ArrayList<CertificateBasicInfoModel> list = new ArrayList<>();
        Enumeration<String> aliases = this.keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if ("X.509".equals(this.keyStore.getCertificate(alias).getType())) {
                X509Certificate certificate = (X509Certificate) this.keyStore.getCertificate(alias);
                try {
                    list.add(new CertificateBasicInfoModel(alias, getSha1Fingerprint(certificate),
                            certificate.getNotBefore(), certificate.getNotAfter(), certificate.getSigAlgName(), certificate));
                } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
                    LOG.error("Failed to add certificate basic info model", e);
                }
            } else {
                list.add(new CertificateBasicInfoModel(alias));
            }
        }
        return list;
    }

    public void addEntry(File file) throws Exception {
        if (this.keyStore == null) {
            throw new KeyStoreException("Trust store is not initialized");
        }

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        try (FileInputStream inputStream = new FileInputStream(file)) {
            X509Certificate certificate = (X509Certificate) cf.generateCertificate(inputStream);
            String newAlias = cleanFileName(FilenameUtils.removeExtension(file.getName()));
            this.keyStore.setCertificateEntry(newAlias, certificate);
            this.keyStore.store(new FileOutputStream(this.sslConfig.getTruststorefile()), this.sslConfig.getTruststorepass().toCharArray());
        }
    }

    public void addEntry(X509Certificate certificate, String newAlias) throws Exception {
        if (this.keyStore == null) {
            throw new Exception("Trust store is not initialized");
        }

        this.keyStore.setCertificateEntry(newAlias, certificate);
        this.keyStore.store(new FileOutputStream(this.sslConfig.getTruststorefile()), this.sslConfig.getTruststorepass().toCharArray());
    }

    public boolean exists(String alias) throws Exception {
        if (this.keyStore == null) {
            throw new Exception("Trust store is not initialized");
        }

        return this.keyStore.containsAlias(alias);
    }

    public boolean exists(String alias) throws Exception {
        if (this.keyStore == null) {
            throw new Exception("Trust store is not initialized");
        }

        return this.keyStore.containsAlias(alias);
    }

    public void updateAlias(String oldAlias, String newAlias) throws Exception {
        if (this.keyStore == null) {
            throw new Exception("Trust store is not initialized");
        }

        if (this.keyStore.containsAlias(oldAlias)) {
            X509Certificate certificate = (X509Certificate) this.keyStore.getCertificate(oldAlias);
            removeEntry(oldAlias);
            addEntry(certificate, newAlias);
            this.keyStore.store(new FileOutputStream(this.sslConfig.getTruststorefile()), this.sslConfig.getTruststorepass().toCharArray());
        }
    }

    public void removeEntry(String alias) throws Exception {
        if (this.keyStore == null) {
            throw new KeyStoreException("Trust store is not initialized");
        }

        this.keyStore.deleteEntry(alias);
        this.keyStore.store(new FileOutputStream(this.sslConfig.getTruststorefile()), this.sslConfig.getTruststorepass().toCharArray());
    }

    private X509TrustManager getTrustManager(KeyStore keyStore) throws Exception {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509", "SunJSSE");
        trustManagerFactory.init(keyStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

        for (TrustManager tm : trustManagers) {
            if (tm instanceof X509TrustManager) {
                return (X509TrustManager) tm;
            }
        }

        throw new CertificateException("Not valid keystore!");
    }

    /**
     * Clean file name from unwanted chars - only alphabetical characters and digits are allowed
     *
     * @param filename input file name to clean
     * @return cleaned filename
     */
    private String cleanFileName(String filename) {
        return filename.replaceAll(this.ALNUM_FILTER_REGEX, "");
    }
}