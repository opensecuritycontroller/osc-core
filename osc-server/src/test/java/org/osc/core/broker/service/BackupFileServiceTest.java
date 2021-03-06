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
package org.osc.core.broker.service;

import static org.junit.Assert.*;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.Properties;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.persistence.EntityManager;
import javax.xml.bind.DatatypeConverter;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.Response;
import org.osc.core.broker.util.crypto.KeyStoreProvider;
import org.osc.core.broker.util.crypto.KeyStoreProvider.KeyStoreProviderException;

public class BackupFileServiceTest {

	// Test classes
	private class TestBackupFileServiceRequest implements Request { }

	private class TestBackupFileServiceResponse implements Response {
        public byte[] iv;
        public SecretKey key;
	}

	/** Test service that simply encodes the input and uses test properties to access key and IV */

	private class TestBackupFileService
			extends BackupFileService<TestBackupFileServiceRequest, TestBackupFileServiceResponse> {

		@Override
		protected TestBackupFileServiceResponse exec(TestBackupFileServiceRequest request, EntityManager em)
				throws Exception {
			TestBackupFileServiceResponse response = new TestBackupFileServiceResponse();

			EncryptionParameters encryptionParameters = getEncryptionParameters();
			response.iv = encryptionParameters.getIV();
			response.key = encryptionParameters.getKey();

			return response;
		}

        @Override
        protected Properties getProperties() {
            return BackupFileServiceTest.this.testProperties;
        }
	}

	private class TestKeyStoreFactory implements KeyStoreProvider.KeyStoreFactory {

		@Override
        public KeyStore createKeyStore() throws KeyStoreProviderException {
            return BackupFileServiceTest.this.testKeyStore;
        }

		@Override
        public void persist(KeyStore keyStore) throws KeyStoreProviderException {
            /* no persistance for tests */ }

	}

	@Mock
	private EntityManager em;
	@Mock
	private KeyStoreProvider keyStoreProviderMock;
	private KeyStoreProvider.KeyStoreFactory testKeyStoreFactory = new TestKeyStoreFactory();

	private Properties testProperties = new Properties();
	private KeyStore testKeyStore;
	private TestBackupFileService target = new TestBackupFileService();

	private static final String BACKUP_KEY_ALIAS = "testBackupFileKeyAlias";
	private static final String BACKUP_KEY_PASSWORD = "testBackupFileKeyPassword";
	private static final String BACKUP_IV_ALIAS = "testBackupFileIVAlias";
	private static final String BACKUP_IV_PASSWORD = "testBackupFileIVPassword";

	private KeyStore createTestKeystore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
	    KeyStore keyStore = KeyStore.getInstance("PKCS12");
	    keyStore.load(null, null);

	    return keyStore;

	}

	private SecretKey generateTestSecretKey() throws NoSuchAlgorithmException {
		KeyGenerator generator = null;
		generator = KeyGenerator.getInstance("AES");
		generator.init(128);
		return generator.generateKey();
	}

	private String generateTestIVHex() {
		byte[] iv = new byte[16];
		new SecureRandom().nextBytes(iv);
		return DatatypeConverter.printHexBinary(iv);
	}

	@Before
    public void setUp() throws Exception {
		this.testProperties.clear();
		this.testProperties.setProperty("db.backup.key.alias", BACKUP_KEY_ALIAS);
        this.testProperties.setProperty("db.backup.key.password", BACKUP_KEY_PASSWORD);
        this.testProperties.setProperty("db.backup.iv.alias", BACKUP_IV_ALIAS);
        this.testProperties.setProperty("db.backup.iv.password", BACKUP_IV_PASSWORD);

        this.testKeyStore = createTestKeystore();
        KeyStoreProvider.setKeyStoreFactory(this.testKeyStoreFactory);
    }

	@Test
	public void testExec_withValidKeyStoreProperties_ProperlyEncodesBackup() throws Exception {
		// Arrange.
		TestBackupFileServiceRequest testRequest = new TestBackupFileServiceRequest();

        SecretKey secretKey = generateTestSecretKey();
        KeyStoreProvider.getInstance().putSecretKey(BACKUP_KEY_ALIAS, secretKey, BACKUP_KEY_PASSWORD);
        String ivHex = generateTestIVHex();
        KeyStoreProvider.getInstance().putPassword(BACKUP_IV_ALIAS, ivHex, BACKUP_IV_PASSWORD);

		// Act.
		TestBackupFileServiceResponse response = this.target.exec(testRequest, this.em);

		// Assert.
		assertEquals(secretKey, response.key);
		assertEquals(ivHex, DatatypeConverter.printHexBinary(response.iv));
	}

	@Test
    public void testIsValidBackupFilename_withValidZipBackupFilename_succeeds() {
		// Arrange.
		String givenFilename = "/example/filename.zip";
		// Act.
        boolean result = this.target.isValidBackupFilename(givenFilename);
		// Assert.
        assertTrue(result);
    }

	@Test
    public void testIsValidBackupFilename_withValidEncryptedBackupFilename_succeeds() {
		// Arrange.
		String givenFilename = "/example/filename.dbb";
		// Act.
        boolean result = this.target.isValidBackupFilename(givenFilename);
		// Assert.
        assertTrue(result);
    }

	@Test
    public void testIsValidBackupFilename_withInvalidBackupFilename_fails() {
		// Arrange.
		String givenFilename = "/example/filename.zip1";
		// Act.
        boolean result = this.target.isValidBackupFilename(givenFilename);
		// Assert.
        assertFalse(result);
    }

	@Test
    public void testIsValidZipBackupFilename_withValidFilename_succeeds() {
		// Arrange.
		String givenFilename = "/example/filename.zip";
		// Act.
        boolean result = this.target.isValidZipBackupFilename(givenFilename);
		// Assert.
        assertTrue(result);
    }

	@Test
    public void testIsValidZipBackupFilename_withInvalidFilename_fails() {
		// Arrange.
		String givenFilename = "/example/filename.zip2";
		// Act.
        boolean result = this.target.isValidZipBackupFilename(givenFilename);
		// Assert.
        assertFalse(result);
    }

	@Test
    public void testIsValidEncryptedBackupFilename_withValidFilename_succeeds() {
		// Arrange.
		String givenFilename = "/example/filename.dbb";
		// Act.
        boolean result = this.target.isValidEncryptedBackupFilename(givenFilename);
		// Assert.
        assertTrue(result);
    }

	@Test
    public void testIsValidEncryptedBackupFilename_withInvalidFilename_fails() {
		// Arrange.
		String givenFilename = "/example/filename.dbb123";
		// Act.
        boolean result = this.target.isValidEncryptedBackupFilename(givenFilename);
		// Assert.
        assertFalse(result);
    }
}