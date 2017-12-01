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

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.osc.core.broker.service.response.CertificateBasicInfoModel;
import org.osc.core.broker.service.ssl.CertificateResolverModel;
import org.osc.core.broker.service.ssl.TruststoreChangedListener;
import org.osc.core.broker.service.ssl.X509TrustManagerApi;
import org.osc.core.broker.util.FileUtil;
import org.osc.core.broker.util.ServerUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = X509TrustManagerApi.class, immediate=true)
public final class X509TrustManagerFactory implements X509TrustManager, X509TrustManagerApi {

    private static final String CERT_CHAIN = "PKI";
    private static final String KEY = "PEM";
    private static final Logger LOG = LoggerFactory.getLogger(X509TrustManagerFactory.class);
    private static final String KEYSTORE_TYPE = "JKS";
    // osctrustore stores public certificates needed to establish SSL connection
    // osctrustore also stores private certificate used by application to enable
    // HTTPS - it's also used to establish connection internally
    public static final String TRUSTSTORE_FILE = "osctrustore.jks";
    // key entry to properties file that contains password
    // osctrustore stores private certificate used by application to enable HTTPS - it's also used to establish connection internally
    private static final String TRUSTSTORE_PASSWORD_ENTRY_KEY = "truststore.password";
    // alias to truststore password entry in PKC#12 password
    private static final String TRUSTSTORE_PASSWORD_ALIAS = "TRUSTSTORE_PASSWORD";
    private static final String INTERNAL_ALIAS = "internal";
    private static final String BAD_KEY_PAIR_ZIP_FILE = "The zip file is expected to contain a PKCS8 PEM file (PEM Extension)"
            + "and a corresponding PKI Certificate path file!";
    private static volatile X509TrustManagerFactory instance = null;
    private final String ALNUM_FILTER_REGEX = "[^a-zA-Z0-9-_\\.]";
    private X509TrustManager trustManager = null;
    private KeyStore keyStore;
    private CertificateInterceptor listener = null;

    /** Listeners for trust store changes (adding/removing/modifying trust store) */
    private LinkedHashSet<TruststoreChangedListener> truststoreChangedListeners = new LinkedHashSet<>();

    public static X509TrustManagerFactory getInstance() {
        if (instance == null) {
            // Do this reflectively to avoid pulling in the OSGi API when
            // starting the server.
            ClassLoader loader = X509TrustManagerFactory.class.getClassLoader();
            if(loader != null) {
                Method m = null;
                try {
                    m = loader.getClass().getMethod("getBundle");
                } catch (Exception e) {
                    // ClassLoader is not an OSGi classloader
                }
                if(m != null && "org.osgi.framework.Bundle".equals(m.getReturnType().getName())) {
                    throw new IllegalStateException("X509TrustManager component is not yet started");
                } else {
                    instance = new X509TrustManagerFactory();
                }
            }

        }

        return instance;
    }

    public X509TrustManagerFactory() {
        try {
            reloadTrustManager();
        } catch (Exception e) {
            LOG.error("Error occurred during TrustManagerFactory initialization", e);
        }
    }

    @Activate
    void start() {
        setInstance(this);
    }

    private static void setInstance(X509TrustManagerFactory mgr) {
        instance = mgr;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return this.trustManager.getAcceptedIssuers();
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        this.trustManager.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        try {
            this.trustManager.checkServerTrusted(chain, authType);
        } catch (CertificateException cx) {
            try {
                X509Certificate x509Certificate = chain[0];
                x509Certificate.checkValidity();
                long unixTimestamp = Instant.now().getEpochSecond();
                CertificateResolverModel resolverModel = new CertificateResolverModel(
                        x509Certificate, String.valueOf(unixTimestamp), getSha1Fingerprint(x509Certificate));
                if (this.listener != null) {
                    this.listener.intercept(resolverModel);
                }
            } catch (NoSuchAlgorithmException e) {
                LOG.error("Cannot generate SHA1 fingerprint for certificate", e);
            }
            throw cx;
        }
    }

    /**
     * Set listener for intercepting SSL certificate
     * @param listener - implementation of handling missing SSL certificate
     */
    public void setListener(CertificateInterceptor listener) {
        this.listener = listener;
    }

    /**
     * Unset listener
     */
    public void clearListener() {
        this.listener = null;
    }

    private void reloadTrustManager() throws Exception {
        this.keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        LOG.debug("Opening trust store file....");
        try (InputStream inputStream = new FileInputStream(TRUSTSTORE_FILE)) {
            this.keyStore.load(inputStream, null); //Password is unnecessary as we are fetching public data
        } catch (FileNotFoundException e) {
            throw new Exception("Failed to load trust store", e);
        } catch (CertificateException e) {
            throw new Exception("Failed to load certificate from trust store", e);
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509", "SunJSSE");
        trustManagerFactory.init(this.keyStore);

        for (TrustManager tm : trustManagerFactory.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                this.trustManager = (X509TrustManager) tm;
                return;
            }
        }

        throw new NoSuchAlgorithmException("No X509TrustManager in TrustManagerFactory");

    }

    @Override
    public List<CertificateBasicInfoModel> getCertificateInfoList() throws Exception {
        reloadTrustManager();
        ArrayList<CertificateBasicInfoModel> list = new ArrayList<>();
        Enumeration<String> aliases = this.keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if ("X.509".equals(this.keyStore.getCertificate(alias).getType())) {
                X509Certificate certificate = (X509Certificate) this.keyStore.getCertificate(alias);
                try {
                    CertificateBasicInfoModel infoModel = new CertificateBasicInfoModel(
                            alias, getSha1Fingerprint(certificate), certificate.getIssuerDN().getName(),
                            certificate.getNotBefore(), certificate.getNotAfter(), certificate.getSigAlgName(),
                            certificateToString(certificate));

                    list.add(infoModel);
                } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
                    LOG.error("Failed to add certificate basic info model", e);
                }
            } else {
                list.add(new CertificateBasicInfoModel(alias));
            }
        }
        return list;
    }

    @Override
    public void addEntry(File file) throws Exception {
        String newAlias = cleanFileName(FilenameUtils.removeExtension(file.getName()));
        addEntry(file, newAlias);
    }

    public void addEntry(File file, String newAlias) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate certificate;
        try (FileInputStream inputStream = new FileInputStream(file)) {
            certificate = (X509Certificate) cf.generateCertificate(inputStream);
        }
        setOrUpdateCertificate(certificate, newAlias);
    }

    @Override
    public void addEntry(X509Certificate certificate, String newAlias) throws Exception {
        if (fingerprintNotExist(getSha1Fingerprint(certificate))) {
            setOrUpdateCertificate(certificate, newAlias);
        } else {
            throw new Exception("Given certificate fingerprint already exists in trust store");
        }
    }

    public void setOrUpdateCertificate(X509Certificate certificate, String newAlias) throws Exception {

        if (INTERNAL_ALIAS.equals(newAlias)) {
            throw new Exception("Certificate alias " + INTERNAL_ALIAS + " is reserved! Change alias supplied or file name.");
        }

        this.keyStore.setCertificateEntry(newAlias, certificate);
        try (FileOutputStream outputStream = new FileOutputStream(TRUSTSTORE_FILE)) {
            this.keyStore.store(outputStream, getTruststorePassword());
        }
        reloadTrustManager();
        notifyTruststoreChanged();

    }

    @Override
    public void replaceInternalCertificate(File zipFile, boolean doReboot) throws Exception {
        // Disabling reboot for testing.

        if (zipFile == null) {
            throw new Exception(BAD_KEY_PAIR_ZIP_FILE);
        }

        Map<String, File> files = unzipFilePair(zipFile);
        File pKeyFile = files.get(KEY);
        File chainFile = files.get(CERT_CHAIN);

        FileUtils.deleteQuietly(zipFile);

        replaceInternalCertificate(pKeyFile, chainFile, doReboot);
    }

    public void replaceInternalCertificate(File pKeyFile, File chainFile, boolean doReboot) throws Exception {
        if (pKeyFile == null || chainFile == null) {
            throw new Exception(BAD_KEY_PAIR_ZIP_FILE);
        }

        LOG.info(format("Replacing internal private/public keys from files %s and %s.",
                 pKeyFile.getName(), chainFile.getName()));

        Certificate[] internalCertificateChain;
        Key internalKey;
        // Loads the .pkipath certificate chain file
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        try (FileInputStream inputStream = new FileInputStream(chainFile)) {
            CertPath certPath = cf.generateCertPath(inputStream);
            List<? extends Certificate> certList = certPath.getCertificates();
            internalCertificateChain = certList.toArray(new Certificate[]{});
        }

        BufferedReader br = new BufferedReader(new FileReader(pKeyFile));
        PEMParser pp = new PEMParser(br);
        PrivateKeyInfo pkInfo = (PrivateKeyInfo) pp.readObject();
        internalKey = new JcaPEMKeyConverter().getPrivateKey(pkInfo);
        pp.close();

        try {
            this.keyStore.setKeyEntry(INTERNAL_ALIAS, internalKey, getTruststorePassword(), internalCertificateChain);
        } catch (KeyStoreException e) {
            throw new Exception("Failed to add persist the new internal certificate!", e);
        }

        try(FileOutputStream outputStream = new FileOutputStream(TRUSTSTORE_FILE)) {
            this.keyStore.store(outputStream, getTruststorePassword());
        }

        reloadTrustManager();
        FileUtils.deleteQuietly(pKeyFile);
        FileUtils.deleteQuietly(chainFile);

        if (doReboot) {
            LOG.info("Replaced internal private/public key! Rebooting system ! ! !");
            ServerUtil.execWithLog("/sbin/reboot");
        }
    }

    public boolean exists(String alias) throws Exception {
        reloadTrustManager();
        return this.keyStore.containsAlias(alias);
    }

    private boolean fingerprintNotExist(final String fingerprint) throws Exception {
        List<CertificateBasicInfoModel> certificateInfoList = getCertificateInfoList();
        return certificateInfoList.stream().noneMatch(entry -> entry.getSha1Fingerprint().equals(fingerprint));
    }

    public void updateAlias(String oldAlias, String newAlias) throws Exception {
        reloadTrustManager();
        if (this.keyStore.containsAlias(oldAlias)) {
            X509Certificate certificate = (X509Certificate) this.keyStore.getCertificate(oldAlias);
            removeEntry(oldAlias);
            addEntry(certificate, newAlias);
            try(FileOutputStream outputStream = new FileOutputStream(TRUSTSTORE_FILE)) {
                this.keyStore.store(outputStream, getTruststorePassword());
            }
        }
        notifyTruststoreChanged();
    }

    public void removeEntry(String alias) throws Exception {
        reloadTrustManager();
        this.keyStore.deleteEntry(alias);
        try (FileOutputStream outputStream = new FileOutputStream(TRUSTSTORE_FILE)) {
            this.keyStore.store(outputStream, getTruststorePassword());
        }
        notifyTruststoreChanged();

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

    @Override
    public String getSha1Fingerprint(X509Certificate cert) throws NoSuchAlgorithmException, CertificateEncodingException, IllegalArgumentException {

        if(cert == null) {
            throw new IllegalArgumentException("Provided certificate is empty");
        }

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] der = cert.getEncoded();
        md.update(der);
        byte[] digest = md.digest();
        return DatatypeConverter.printHexBinary(digest);
    }

    /**
     * Interface which allows to intercept missing certificates. Should be defined before calling REST API
     */
    public interface CertificateInterceptor {
        void intercept(CertificateResolverModel model);
    }

    private char[] getTruststorePassword() throws Exception {
        // password to keystore to retrieve truststore manager password
        return getSecurityProperty(TRUSTSTORE_PASSWORD_ENTRY_KEY).toCharArray();
    }

    private String getSecurityProperty(String entryKey) throws Exception {
        // output property
        String property;

        Properties properties = new Properties();
        try {
            properties.load(getClass().getResourceAsStream(EncryptionUtil.SECURITY_PROPS_RESOURCE_PATH));
        } catch (IOException e) {
            throw new Exception("Failed to load entry from security properties.", e);
        }
        property = properties.getProperty(entryKey);

        if (property == null || property.isEmpty()) {
            throw new Exception("No entry defined in properties.");
        }

        return property;
    }

    private void notifyTruststoreChanged() {
        List<TruststoreChangedListener> toCall;
        synchronized (this) {
            toCall = new ArrayList<>(this.truststoreChangedListeners);
        }
        toCall.stream().forEach(listener -> listener.truststoreChanged());
    }

    @Reference(policy=ReferencePolicy.DYNAMIC, cardinality=ReferenceCardinality.MULTIPLE)
    synchronized void addTruststoreChangedListener(TruststoreChangedListener listener) {
        this.truststoreChangedListeners.add(listener);
    }

    synchronized void removeTruststoreChangedListener(TruststoreChangedListener listener) {
        this.truststoreChangedListeners.remove(listener);
    }

    @Override
    public String certificateToString(X509Certificate certificate) {
        StringWriter sw = new StringWriter();
        try {
            sw.write("-----BEGIN CERTIFICATE-----\n");
            sw.write(DatatypeConverter.printBase64Binary(certificate.getEncoded()).replaceAll("(.{64})", "$1\n"));
            sw.write("\n-----END CERTIFICATE-----");
        } catch (CertificateEncodingException e) {
            LOG.error("Cannot encode certificate", e);
        }
        return sw.toString();
    }

    private Map<String, File> unzipFilePair(File zipFile) throws Exception {

        List<File> keyPairFiles = FileUtil.unzip(zipFile);
        if (keyPairFiles.size() != 2) {
            throw new Exception(BAD_KEY_PAIR_ZIP_FILE);
        }

        Map<String, File> retVal = new HashMap<>();
        for (File file : keyPairFiles) {
            if (file.getName().toUpperCase().endsWith(KEY)) {
                retVal.put(KEY, file);
            } else {
                retVal.put(CERT_CHAIN, file);
            }
        }

        if (retVal.size() != 2) {
            throw new Exception(BAD_KEY_PAIR_ZIP_FILE);
        }

        return retVal;
    }
}
