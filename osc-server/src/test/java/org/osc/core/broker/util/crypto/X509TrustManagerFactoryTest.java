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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osc.core.broker.service.response.CertificateBasicInfoModel;

@RunWith(MockitoJUnitRunner.class)
public class X509TrustManagerFactoryTest {
    private static final String TEST_CERT_FILE_NAME = "testcertificate.crt";
    private static final String TEST_INTERNAL_CERT_FILE_NAME = "testinternalcert.jks";
    private static final String TEST_TRUSTSTORE_FILE_NAME = "osctrustore.jks";
    private static final String TEST_CERT_ALIAS = "testcertificate";
    private File testCertFile;
    private File testTrustStoreFile;
    private File testInternalCertFile;

    private X509TrustManagerFactory factory;

    @Before
    public void setup() throws IOException {

        this.testCertFile = new File(TEST_CERT_FILE_NAME);
        InputStream tmpInputStream = getClass().getClassLoader().getResourceAsStream(TEST_CERT_FILE_NAME);
        FileUtils.copyToFile(tmpInputStream, this.testCertFile);

        this.testTrustStoreFile = new File(TEST_TRUSTSTORE_FILE_NAME);
        tmpInputStream = getClass().getClassLoader().getResourceAsStream(TEST_TRUSTSTORE_FILE_NAME);
        FileUtils.copyToFile(tmpInputStream, this.testTrustStoreFile);

        this.testInternalCertFile = new File(TEST_INTERNAL_CERT_FILE_NAME);
        tmpInputStream = getClass().getClassLoader().getResourceAsStream(TEST_INTERNAL_CERT_FILE_NAME);
        FileUtils.copyToFile(tmpInputStream, this.testInternalCertFile);

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

        if (this.testInternalCertFile != null) {
            this.testInternalCertFile.delete();
        }
    }

    @Test
    public void testAddEntry_WithFile_ShouldAddCertificate() throws Exception {
        assertFalse(this.factory.exists(TEST_CERT_ALIAS));
        this.factory.addEntry(this.testCertFile);
        assertTrue(this.factory.exists(TEST_CERT_ALIAS));
    }

    @Test
    public void testReplaceInternal_WithInternalCert_ShouldReplace() throws Exception{

        // Arrange.
        Optional<CertificateBasicInfoModel> internalCertInfo = this.factory.getCertificateInfoList().stream()
                                                                .filter(m -> m.getAlias().equals("internal"))
                                                                .findFirst();

        assertTrue("Bad test setup. No internal certificate in test truststore!", internalCertInfo.isPresent());
        String replaceFingerprint = null;
        try (FileInputStream fis = new FileInputStream(this.testInternalCertFile)) {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(fis, null);
            X509Certificate replacementCert = (X509Certificate) keystore.getCertificate("internal");
            assertNotNull(replacementCert);
            replaceFingerprint = this.factory.getSha1Fingerprint(replacementCert);
        }

        // Act.
        this.factory.replaceInternalCertificate(this.testInternalCertFile);

        // Assert.
        internalCertInfo = this.factory.getCertificateInfoList().stream()
                .filter(m -> m.getAlias().equals("internal"))
                .findFirst();

        assertTrue("Internal certificate missing after replace call!", internalCertInfo.isPresent());
        Assert.assertEquals("Internal certificate not replaced!", replaceFingerprint,
                                internalCertInfo.get().getSha1Fingerprint());
    }
}
