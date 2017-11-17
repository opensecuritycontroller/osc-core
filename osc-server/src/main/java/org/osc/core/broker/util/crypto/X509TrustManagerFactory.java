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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FilenameUtils;
import org.osc.core.broker.service.response.CertificateBasicInfoModel;
import org.osc.core.broker.service.ssl.CertificateResolverModel;
import org.osc.core.broker.service.ssl.TruststoreChangedListener;
import org.osc.core.broker.service.ssl.X509TrustManagerApi;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = X509TrustManagerApi.class, immediate=true)
public final class X509TrustManagerFactory implements X509TrustManager, X509TrustManagerApi {

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
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        String newAlias = cleanFileName(FilenameUtils.removeExtension(file.getName()));
        X509Certificate certificate;
        try (FileInputStream inputStream = new FileInputStream(file)) {
            certificate = (X509Certificate) cf.generateCertificate(inputStream);
        }
        this.keyStore.setCertificateEntry(newAlias, certificate);
        try (FileOutputStream outputStream = new FileOutputStream(TRUSTSTORE_FILE)) {
            this.keyStore.store(outputStream, getTruststorePassword());
        }
        reloadTrustManager();
        notifyTruststoreChanged();
    }

    @Override
    public void addEntry(X509Certificate certificate, String newAlias) throws Exception {
        if (fingerprintNotExist(getSha1Fingerprint(certificate))) {
            this.keyStore.setCertificateEntry(newAlias, certificate);
            try (FileOutputStream outputStream = new FileOutputStream(TRUSTSTORE_FILE)) {
                this.keyStore.store(outputStream, getTruststorePassword());
            }
            reloadTrustManager();
            notifyTruststoreChanged();
        } else {
            throw new Exception("Given certificate fingerprint already exists in trust store");
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
        String passwordPassword = getSecurityProperty(TRUSTSTORE_PASSWORD_ENTRY_KEY);
        return KeyStoreProvider.getInstance().getPassword(TRUSTSTORE_PASSWORD_ALIAS,passwordPassword).toCharArray();
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
}
