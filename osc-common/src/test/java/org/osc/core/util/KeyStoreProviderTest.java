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
package org.osc.core.util;

import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.osc.core.util.KeyStoreProvider.KeyStoreProviderException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class KeyStoreProviderTest {

	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	KeyStore testKeyStore;
	KeyStoreProvider.KeyStoreFactory testKeyStoreFactory;

	static SecretKey testKey;

	@Before
	public void setUpClass() throws NoSuchAlgorithmException {
		testKey = generateTestKey();
	}

	@Before
	public void setUp() throws Exception {
		testKeyStore = KeyStore.getInstance("PKCS12");
		testKeyStore.load(null, null);
		testKeyStoreFactory = mock(KeyStoreProvider.KeyStoreFactory.class);
		when(testKeyStoreFactory.createKeyStore()).thenReturn(testKeyStore);
		KeyStoreProvider.setKeyStoreFactory(testKeyStoreFactory);
	}

	@Test
	public void testPutPassword_withValidInput_storesPasswordProperly() throws Exception {
		// Arrange.
		String testEntryAlias = "TEST_ENTRY_ALIAS";
		String testEntryPassword = "TEST_ENTRY_PASSWORD";
		String testPasswordToStore = "TEST_PASSWORD_TO_STORE";
		
		// Act.
		KeyStoreProvider.getInstance().putPassword(testEntryAlias, testPasswordToStore, testEntryPassword);
		String passwordFromKeyStore = KeyStoreProvider.getInstance().getPassword(testEntryAlias, testEntryPassword);
		
		// Assert.
		assertEquals("Password obtained from keystore is differnt then the one that was stored", testPasswordToStore, passwordFromKeyStore);
	}
	
	@Test
	public void testPutPassword_withNullAlias_throwsKeyStoreProviderException() throws Exception {
		// Arrange.
		String nullAlias = null;
		String testEntryPassword = "TEST_ENTRY_PASSWORD";
		String testPasswordToStore = "TEST_PASSWORD_TO_STORE";
		
		// Assert.
		exception.expect(KeyStoreProviderException.class);
		
		// Act.
		KeyStoreProvider.getInstance().putPassword(nullAlias, testPasswordToStore, testEntryPassword);
		
		
	}
	
	@Test
	public void testPutPassword_withNullPassword_throwsKeyStoreProviderException() throws Exception {
		// Arrange.
		String nullAlias = "TEST_ENTRY_ALIAS";
		String testEntryPassword = "TEST_ENTRY_PASSWORD";
		String testPasswordToStore = null;
		
		// Assert.
		exception.expect(KeyStoreProviderException.class);
		
		// Act.
		KeyStoreProvider.getInstance().putPassword(nullAlias, testPasswordToStore, testEntryPassword);
	}
	
	@Test
	public void testPutPassword_withNullEntryPassword_throwsKeyStoreProviderException() throws Exception {
		// Arrange.
		String nullAlias = "TEST_ENTRY_ALIAS";
		String testEntryPassword = null;
		String testPasswordToStore = "TEST_PASSWORD_TO_STORE";
		
		// Assert.
		exception.expect(KeyStoreProviderException.class);
		
		// Act.
		KeyStoreProvider.getInstance().putPassword(nullAlias, testPasswordToStore, testEntryPassword);
	}
	
	@Test
	public void testGetPassword_withNotExisitingAlias_returnsNull() throws Exception {
		// Arrange.
		String testEntryAlias = "TEST_NON_EXISTING_ENTRY_ALIAS";
		String testEntryPassword = "TEST_ENTRY_PASSWORD";
		
		// Act.
		String password = KeyStoreProvider.getInstance().getPassword(testEntryAlias,testEntryPassword);
		
		// Assert.
		assertNull(password);
	}
	
	
	@Test
	public void testGetPassword_withNullEntryPassword_throwsKeyStoreProviderException() throws Exception {
		// Arrange.
		String testEntryAlias = "TEST_ENTRY_ALIAS";
		String testEntryPassword = null;
		
		// Assert.
		exception.expect(KeyStoreProviderException.class);
		
		// Act.
		KeyStoreProvider.getInstance().getPassword(testEntryAlias,testEntryPassword);
	}
	
	@Test
	public void testGetPassword_withInvalidEntryPassword_throwsKeyStoreProviderException() throws Exception {
		// Arrange.
		String testEntryAlias = "TEST_ENTRY_ALIAS";
		String testEntryPassword = "TEST_ENTRY_PASSWORD";
		String testInvalidEntryPassword = "TEST_INVALID_ENTRY_PASSWORD";
		String testPasswordToStore = "TEST_PASSWORD_TO_STORE";
		
		// Assert.
		exception.expect(KeyStoreProviderException.class);
				
		// Act.
		KeyStoreProvider.getInstance().putPassword(testEntryAlias, testPasswordToStore, testEntryPassword);
		KeyStoreProvider.getInstance().getPassword(testEntryAlias, testInvalidEntryPassword);
	}
	
	@Test
	public void testPutSecretKey_withValidInput_storesSecretKeyProperly() throws Exception {
		// Arrange.
		String testEntryAlias = "TEST_ENTRY_ALIAS";
		String testEntryPassword = "TEST_ENTRY_PASSWORD";

		// Act.
		KeyStoreProvider.getInstance().putSecretKey(testEntryAlias, testKey, testEntryPassword);
		SecretKey keyFromKeyStore = KeyStoreProvider.getInstance().getSecretKey(testEntryAlias, testEntryPassword);
		
		// Assert.
		assertEquals("Password obtained from keystore is differnt then the one that was stored", testKey, keyFromKeyStore);
	}
	
	@Test
	public void testPutSecretKey_withNullAlias_throwsKeyStoreProviderException() throws Exception {
		// Arrange.
		String testEntryAlias = null;
		String testEntryPassword = "TEST_ENTRY_PASSWORD";

		// Assert.
		exception.expect(KeyStoreProviderException.class);
		
		// Act.
		KeyStoreProvider.getInstance().putSecretKey(testEntryAlias, testKey, testEntryPassword);
	}
	
	@Test
	public void testPutSecretKey_withNullSecretKey_throwsKeyStoreProviderException() throws Exception {
		// Arrange.
		String testEntryAlias = "TEST_ENTRY_ALIAS";
		String testEntryPassword = "TEST_ENTRY_PASSWORD";
		SecretKey testKey = null;
		
		// Assert.
		exception.expect(KeyStoreProviderException.class);
		
		// Act.
		KeyStoreProvider.getInstance().putSecretKey(testEntryAlias, testKey, testEntryPassword);
	}
	
	@Test
	public void testPutSecretKey_withNullEntryPassword_throwsKeyStoreProviderException() throws Exception {
		// Arrange.
		String testEntryAlias = "TEST_ENTRY_ALIAS";
		String testEntryPassword = null;

		// Assert.
		exception.expect(KeyStoreProviderException.class);
		
		// Act.
		KeyStoreProvider.getInstance().putSecretKey(testEntryAlias, testKey, testEntryPassword);
	}
	
	@Test
	public void testGetSecretKey_withNotExisitingAlias_returnsNull() throws Exception {
		// Arrange.
		String testEntryAlias = "TEST_NON_EXISTING_ENTRY_ALIAS";
		String testEntryPassword = "TEST_ENTRY_PASSWORD";
		
		// Act.
		SecretKey key = KeyStoreProvider.getInstance().getSecretKey(testEntryAlias,testEntryPassword);
		
		// Assert.
		assertNull(key);
	}
	
	
	@Test
	public void testGetSecretKey_withNullEntryPassword_throwsKeyStoreProviderException() throws Exception {
		// Arrange.
		String testEntryAlias = "TEST_NON_EXISTING_ENTRY_ALIAS";
		String testEntryPassword = null;
		
		// Assert.
		exception.expect(KeyStoreProviderException.class);
				
		// Act.
		KeyStoreProvider.getInstance().getSecretKey(testEntryAlias,testEntryPassword);
	}
	
	@Test
	public void testGetSecretKey_withInvalidEntryPassword_throwsKeyStoreProviderException() throws Exception {
		// Arrange.
		String testEntryAlias = "TEST_ENTRY_ALIAS";
		String testEntryPassword = "TEST_ENTRY_PASSWORD";
		String testInvalidEntryPassword = "TEST_INVALID_ENTRY_PASSWORD";

		// Assert.
		exception.expect(KeyStoreProviderException.class);
				
		// Act.
		KeyStoreProvider.getInstance().putSecretKey(testEntryAlias, testKey, testEntryPassword);
		KeyStoreProvider.getInstance().getSecretKey(testEntryAlias, testInvalidEntryPassword);
	}

	private static SecretKey generateTestKey() throws NoSuchAlgorithmException {
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(128, SecureRandom.getInstance("SHA1PRNG"));
		return keyGen.generateKey();
	}
}