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
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class X509TrustStoreUpgradeUtil {

    private static final Logger LOG = LoggerFactory.getLogger(X509TrustStoreUpgradeUtil.class);

    // protected for access by test class
    protected static final String PRE_0_8_TRUSTSTORE_FILE = "vmidctruststore.jks";
    protected static final String PRE_0_8_INTERNAL_KEYSTORE_FILE = "vmidcKeyStore.jks";
    protected static final String NEW_0_8_TRUSTSTORE_FILE = X509TrustManagerFactory.TRUSTSTORE_FILE ;

    private static final String INTERNAL_KEYSTORE_PASSWORD_ENTRY = "internal.keystore.password";
    private static final String PRE_0_8_KEYSTORE_TYPE = "JKS";

    public static void upgradeTrustStore() throws Exception {
        upgradeTo_0_8();
    }

    private static void upgradeTo_0_8() throws Exception {
        File trustStore08 = new File(NEW_0_8_TRUSTSTORE_FILE);
        if (trustStore08.exists()) {
            return;
        }

        File trustStorePre08 = new File(PRE_0_8_TRUSTSTORE_FILE);
        File internalKeyStorePre08 = new File(PRE_0_8_INTERNAL_KEYSTORE_FILE);

        if (trustStorePre08.exists() != internalKeyStorePre08.exists()) {
            LOG.error("Installation may be corrupted? One old certificate file exists but not the other.");
        }

        if (!trustStorePre08.exists() || !internalKeyStorePre08.exists()) {
            return;
        }

        KeyStore internalKeyStore = loadPre08Keystore(PRE_0_8_INTERNAL_KEYSTORE_FILE);
        KeyStore oldTrustStore = loadPre08Keystore(PRE_0_8_TRUSTSTORE_FILE);

        Key internalKey;
        Certificate[] internalCertificateChain;
        try {
             internalKey = internalKeyStore.getKey("vmidckeystore", getInternalKeystorePassword());
             internalCertificateChain = internalKeyStore.getCertificateChain("vmidckeystore");
        } catch (ClassCastException e) {
            throw new Exception("Internal certificate file does not contain expected certificate!", e);
        } catch (KeyStoreException e) {
            throw new Exception("Internal certificate file is corrupted!", e);
        }

        oldTrustStore.setKeyEntry("vmidckeystore", internalKey, getInternalKeystorePassword(), internalCertificateChain);

        try (FileOutputStream trustStoreFile = new FileOutputStream(NEW_0_8_TRUSTSTORE_FILE)) {
            oldTrustStore.store(trustStoreFile, getInternalKeystorePassword());
        }
    }

    private static KeyStore loadPre08Keystore(String fileName) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(PRE_0_8_KEYSTORE_TYPE);

        try (InputStream inputStream = new FileInputStream(fileName)) {
            keyStore.load(inputStream, getInternalKeystorePassword());
        } catch (FileNotFoundException e) {
            throw new Exception("Failed to load trust store", e);
        } catch (CertificateException e) {
            throw new Exception("Failed to load certificate from trust store", e);
        }
        return keyStore;
    }

    private static char[] getInternalKeystorePassword() throws Exception {
        Properties properties = new Properties();
        try {
            properties.load(X509TrustStoreUpgradeUtil.class.getResourceAsStream(EncryptionUtil.SECURITY_PROPS_RESOURCE_PATH));
        } catch (IOException e) {
            throw new Exception("Failed to load entry from security properties.", e);
        }

        return properties.getProperty(INTERNAL_KEYSTORE_PASSWORD_ENTRY).toCharArray();
    }
}
