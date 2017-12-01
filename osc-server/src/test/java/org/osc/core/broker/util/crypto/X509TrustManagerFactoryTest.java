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

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class X509TrustManagerFactoryTest {
    private static final String TEST_CERT_FILE_NAME = "testcertificate.crt";
    private static final String TEST_TRUSTSTORE_FILE_NAME = "osctrustore.jks";
    private static final String TEST_CERT_ALIAS = "testcertificate";

    private File testCertFile;
    private File testTrustStoreFile;
    private File testZipFile;
    private static final String TEST_ZIP_FILE = "oscx509test.zip";
    private static final String TEST_PRIVATE_KEY_FILE = "oscx509test.pem";
    private static final String TEST_CHAIN_FILE = "oscx509test.pkipath";
    private File testPrivateKeyFile;
    private File testChainFile;

    private X509TrustManagerFactory factory;

    @Before
    public void setup() throws IOException {

        this.testCertFile = new File(TEST_CERT_FILE_NAME);
        InputStream tmpInputStream = getClass().getClassLoader().getResourceAsStream(TEST_CERT_FILE_NAME);
        FileUtils.copyToFile(tmpInputStream, this.testCertFile);

        this.testTrustStoreFile = new File(TEST_TRUSTSTORE_FILE_NAME);
        tmpInputStream = getClass().getClassLoader().getResourceAsStream(TEST_TRUSTSTORE_FILE_NAME);
        FileUtils.copyToFile(tmpInputStream, this.testTrustStoreFile);

        this.testPrivateKeyFile = new File(TEST_PRIVATE_KEY_FILE);
        tmpInputStream = getClass().getClassLoader().getResourceAsStream(TEST_PRIVATE_KEY_FILE);
        FileUtils.copyToFile(tmpInputStream, this.testPrivateKeyFile);

        this.testChainFile = new File(TEST_CHAIN_FILE);
        tmpInputStream = getClass().getClassLoader().getResourceAsStream(TEST_CHAIN_FILE);
        FileUtils.copyToFile(tmpInputStream, this.testChainFile);

        this.testZipFile = new File(TEST_ZIP_FILE);
        tmpInputStream = getClass().getClassLoader().getResourceAsStream(TEST_ZIP_FILE);
        FileUtils.copyToFile(tmpInputStream, this.testZipFile);

        this.factory = X509TrustManagerFactory.getInstance();
    }

    @After
    public void tearDown() {
        if (this.testCertFile != null) {
            this.testCertFile.delete();
        }

        if (this.testTrustStoreFile != null) {
            this.testTrustStoreFile.delete();
        }

        if (this.testPrivateKeyFile != null) {
            this.testPrivateKeyFile.delete();
        }

        if (this.testChainFile != null) {
            this.testChainFile.delete();
        }

        if (this.testZipFile != null) {
            this.testZipFile.delete();
        }
    }

    @Test
    public void testAddEntry_WithJksFile_ShouldAddCertificate() throws Exception {
        assertFalse(this.factory.exists(TEST_CERT_ALIAS));
        this.factory.addEntry(this.testCertFile);
        assertTrue(this.factory.exists(TEST_CERT_ALIAS));
    }

    @Test
    public void testAddKeyPair_FromZip_ShouldSucceed() throws Exception {
        // Arrange.
        BufferedReader br = new BufferedReader(new FileReader(this.testPrivateKeyFile));
        PEMParser pp = new PEMParser(br);
        PrivateKeyInfo pkInfo = (PrivateKeyInfo) pp.readObject();
        PrivateKey pKey = new JcaPEMKeyConverter().getPrivateKey(pkInfo);
        pp.close();

        byte[] pKeyEncoded = pKey.getEncoded();

        KeyStore origKeystore = KeyStore.getInstance("JKS");
        char[] password = "abc12345".toCharArray();
        try (FileInputStream inputStream = new FileInputStream(this.testTrustStoreFile)) {
            origKeystore.load(inputStream, password);
        }

        byte[] origInternalEncoded = origKeystore.getKey("internal", password).getEncoded();

        assertNotEquals("Bad test setup. Replacement key already in original truststore.",
                         ArrayUtils.hashCode(origInternalEncoded), ArrayUtils.hashCode(pKeyEncoded));

        // Act.
        this.factory.replaceInternalCertificate(this.testZipFile, false);

        // Assert.
        KeyStore resultingKeystore = KeyStore.getInstance("JKS");
        try (FileInputStream inputStream = new FileInputStream(this.testTrustStoreFile)) {
            resultingKeystore.load(inputStream, password);
        }

        assertNotNull("Private key missing from resulting truststore", resultingKeystore.getKey("internal", password));
        assertArrayEquals("Private key not replaced in resulting truststore", pKey.getEncoded(),
                          resultingKeystore.getKey("internal", password).getEncoded());
    }
}
