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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.osc.core.broker.service.api.server.ArchiveApi;
import org.osc.core.broker.service.api.server.LoggingApi;
import org.osc.core.broker.service.archive.ArchiveUtil;

public class X509TrustManagerFactoryTest {

    private static final String TEST_CERT_FILE_NAME = "testcertificate.crt";
    private static final String TEST_TRUSTSTORE_FILE_NAME = "osctrustore.jks";
    private static final String TEST_CERT_ALIAS = "testcertificate";

    private static final String TEST_X509PEM_ZIP_FILE = "oscx509test_x509pem.zip";
    private static final String TEST_PKICHAIN_ZIP_FILE_NAME = "oscx509test.zip";
    private static final String TEST_PRIVATE_KEY_FILE_NAME = "oscx509test.pem";

    private File testCertFile;
    private File testTrustStoreFile;
    private File testPKIChainZipFile;
    private File testX509PEMZipFile;
    private File testPrivateKeyFile;

    @Mock
    private LoggingApi loggingApi;

    @Spy
    @InjectMocks
    private ArchiveApi archiveUtil = new ArchiveUtil();

    @InjectMocks
    private X509TrustManagerFactory factory;

    @Before
    public void setup() throws IOException {

        this.testCertFile = new File(TEST_CERT_FILE_NAME);
        InputStream tmpInputStream = getClass().getClassLoader().getResourceAsStream(TEST_CERT_FILE_NAME);
        FileUtils.copyToFile(tmpInputStream, this.testCertFile);

        this.testTrustStoreFile = new File(TEST_TRUSTSTORE_FILE_NAME);
        tmpInputStream = getClass().getClassLoader().getResourceAsStream(TEST_TRUSTSTORE_FILE_NAME);
        FileUtils.copyToFile(tmpInputStream, this.testTrustStoreFile);

        this.testPrivateKeyFile = new File(TEST_PRIVATE_KEY_FILE_NAME);
        tmpInputStream = getClass().getClassLoader().getResourceAsStream(TEST_PRIVATE_KEY_FILE_NAME);
        FileUtils.copyToFile(tmpInputStream, this.testPrivateKeyFile);

        this.testPKIChainZipFile = new File(TEST_PKICHAIN_ZIP_FILE_NAME);
        tmpInputStream = getClass().getClassLoader().getResourceAsStream(TEST_PKICHAIN_ZIP_FILE_NAME);
        FileUtils.copyToFile(tmpInputStream, this.testPKIChainZipFile);

        this.testX509PEMZipFile = new File(TEST_X509PEM_ZIP_FILE);
        tmpInputStream = getClass().getClassLoader().getResourceAsStream(TEST_X509PEM_ZIP_FILE);
        FileUtils.copyToFile(tmpInputStream, this.testX509PEMZipFile);

        this.factory = X509TrustManagerFactory.getInstance();

        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
        if (this.testCertFile.exists()) {
            this.testCertFile.delete();
        }

        if (this.testTrustStoreFile.exists()) {
            this.testTrustStoreFile.delete();
        }

        if (this.testPrivateKeyFile.exists()) {
            this.testPrivateKeyFile.delete();
        }

        if (this.testPKIChainZipFile.exists()) {
            this.testPKIChainZipFile.delete();
        }

        if (this.testX509PEMZipFile.exists()) {
            this.testX509PEMZipFile .delete();
        }
    }

    @Test
    public void testAddEntry_WithJksFile_ShouldAddCertificate() throws Exception {
        assertFalse(this.factory.exists(TEST_CERT_ALIAS));
        this.factory.addEntry(this.testCertFile);
        assertTrue(this.factory.exists(TEST_CERT_ALIAS));
    }

    @Test
    public void testAddKeyPair_FromZipWithPKI_ShouldSucceed() throws Exception {
        doTestAddKeyPair(this.testPKIChainZipFile);
    }

    @Test
    public void testAddKeyPair_FromZipWithX509PEM_ShouldSucceed() throws Exception {
        doTestAddKeyPair(this.testX509PEMZipFile);
    }

    private void doTestAddKeyPair(File zipFile) throws Exception {
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
        this.factory.replaceInternalCertificate(zipFile, false);

        // Assert.
        KeyStore resultingKeystore = KeyStore.getInstance("JKS");
        try (FileInputStream inputStream = new FileInputStream(this.testTrustStoreFile)) {
            resultingKeystore.load(inputStream, password);
        }

        assertNotNull("Private key missing from resulting truststore", resultingKeystore.getKey("internal", password));
        assertArrayEquals("Private key not replaced in resulting truststore", pKeyEncoded,
                          resultingKeystore.getKey("internal", password).getEncoded());

    }
}
