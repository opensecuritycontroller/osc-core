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
package org.osc.core.rest.client.crypto;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.osc.core.rest.client.crypto.model.CertificateBasicInfoModel;
import org.osc.core.rest.client.crypto.model.CertificateResolverModel;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
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
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class X509TrustManagerFactory implements X509TrustManager {

    private static final Logger LOG = Logger.getLogger(X509TrustManagerFactory.class);
    private static final String TRUSTSTOREFILE = "vmidctruststore.jks";
    private static final String TRUSTSTOREPASSWORD = "abc12345";

    private static X509TrustManagerFactory instance = null;
    private final String ALNUM_FILTER_REGEX = "[^a-zA-Z0-9-_\\.]";
    private X509TrustManager trustManager = null;
    private KeyStore keyStore;
    private SslConfig sslConfig = new SslConfig(TRUSTSTOREFILE, TRUSTSTOREPASSWORD);
    private HashMap<String, CertificateResolverModel> connectionCertificates = new HashMap<>();

    public static X509TrustManagerFactory getInstance() throws Exception {
        if (instance == null) {
            instance = new X509TrustManagerFactory();
        } else {
            instance.reloadTrustManager();
        }
        return instance;
    }

    private X509TrustManagerFactory() throws Exception {
        try {
            reloadTrustManager();
        } catch (Exception e) {
            LOG.error("Error during loading truststore");
        }
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
                //x509Certificate.checkValidity(); //:TODO uncomment if needed feature
                long unixTimestamp = Instant.now().getEpochSecond();
                CertificateResolverModel resolverModel = new CertificateResolverModel(x509Certificate, String.valueOf(unixTimestamp), getSha1Fingerprint(x509Certificate));
                this.connectionCertificates.put(resolverModel.getSha1(), resolverModel);
            } catch (NoSuchAlgorithmException e) {
                LOG.error("Cannot generate SHA1 fingerprint for certificate", e);
            }
            throw cx;
        }
    }

    private void reloadTrustManager() throws Exception {
        this.keyStore = KeyStore.getInstance(this.sslConfig.getTruststoretype());
        LOG.debug("Opening trust store file....");
        try (InputStream inputStream = new FileInputStream(this.sslConfig.getTruststorefile())) {
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
                            certificate);

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

    public void addEntry(File file) throws Exception {
        reloadTrustManager();

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        try (FileInputStream inputStream = new FileInputStream(file)) {
            X509Certificate certificate = (X509Certificate) cf.generateCertificate(inputStream);
            String newAlias = cleanFileName(FilenameUtils.removeExtension(file.getName()));
            this.keyStore.setCertificateEntry(newAlias, certificate);
            this.keyStore.store(new FileOutputStream(this.sslConfig.getTruststorefile()), this.sslConfig.getTruststorepass().toCharArray());
        }
    }

    public void addEntry(X509Certificate certificate, String newAlias) throws Exception {
        reloadTrustManager();

        if(checkFingerprintNotExist(getSha1Fingerprint(certificate))){
            this.keyStore.setCertificateEntry(newAlias, certificate);
            this.keyStore.store(new FileOutputStream(this.sslConfig.getTruststorefile()), this.sslConfig.getTruststorepass().toCharArray());
        } else {
            throw new Exception("Given certificate fingerprint already exists in trust store");
        }
    }

    public boolean exists(String alias) throws Exception {
        reloadTrustManager();

        return this.keyStore.containsAlias(alias);
    }

    private boolean checkFingerprintNotExist(final String fingerprint) throws Exception {
        List<CertificateBasicInfoModel> certificateInfoList = this.getCertificateInfoList();
        return certificateInfoList.stream().noneMatch(entry -> entry.getSha1Fingerprint().equals(fingerprint));
    }

    public void updateAlias(String oldAlias, String newAlias) throws Exception {
        reloadTrustManager();

        if (this.keyStore.containsAlias(oldAlias)) {
            X509Certificate certificate = (X509Certificate) this.keyStore.getCertificate(oldAlias);
            removeEntry(oldAlias);
            addEntry(certificate, newAlias);
            this.keyStore.store(new FileOutputStream(this.sslConfig.getTruststorefile()), this.sslConfig.getTruststorepass().toCharArray());
        }
    }

    public void removeEntry(String alias) throws Exception {
        reloadTrustManager();

        this.keyStore.deleteEntry(alias);
        this.keyStore.store(new FileOutputStream(this.sslConfig.getTruststorefile()), this.sslConfig.getTruststorepass().toCharArray());
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

    public static String getSha1Fingerprint(X509Certificate cert) throws NoSuchAlgorithmException, CertificateEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] der = cert.getEncoded();
        md.update(der);
        byte[] digest = md.digest();
        return DatatypeConverter.printHexBinary(digest);
    }

    public List<CertificateResolverModel> getConnectionCertificates() {
        List<CertificateResolverModel> modelList = this.connectionCertificates.values().stream().collect(Collectors.toList());
        this.connectionCertificates.clear();
        return modelList;
    }
}