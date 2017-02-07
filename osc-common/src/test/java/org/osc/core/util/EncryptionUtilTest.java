package org.osc.core.util;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.osc.core.util.encryption.EncryptionException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.crypto.*")
@PrepareForTest(KeyStoreProvider.class)
public class EncryptionUtilTest {

	@Mock
	KeyStoreProvider keyStoreProviderMock;

	private String unEncryptedMessage = "helloworld";
	private String aesCtrEncryptedMessage = "af5b59f52f5c3f0a77c6ba3bae08c1fe:26255b1fabfecfc469af";

	private SecretKey key = generateTestAESGCMKey();
	private SecretKey invalidKey = generateTestAESGCMKey();
	private byte[] plainText;
	private byte[] iv;
	private byte[] aad;

	@Before
	public void setUp() throws Exception {
		// Arrange.
		plainText = "Some text to encrypt".getBytes();
		iv = new byte[16];
		aad = "Some additional authentication data".getBytes();
		new SecureRandom().nextBytes(iv);

		PowerMockito.mockStatic(KeyStoreProvider.class);
		Mockito.when(KeyStoreProvider.getInstance()).thenReturn(keyStoreProviderMock);

		Mockito.when(keyStoreProviderMock.getPassword(Mockito.matches("AesCtrKey"), Mockito.anyString())).thenReturn("1234567890abcdef1234567890abcdef");
	}

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	/** To check the valid behavior of encryption with valid string message */
	@Test
	public void testEncryptPbkdf2_WithValidMessage_ExpectsPbkdf2Hash() {
		String encryption = EncryptionUtil.encryptPbkdf2(unEncryptedMessage);
		assertTrue(encryption.startsWith("4000"));
		assertEquals(encryption.length(), 102);
	}

	@Test
	public void testValidatePbkdf2_WithValidMessage_ExpectsSuccess() {
		String encryption = EncryptionUtil.encryptPbkdf2(unEncryptedMessage);
		assertTrue(EncryptionUtil.validatePbkdf2(unEncryptedMessage, encryption));
	}

	@Test
	public void testEncryptPbkdf2_WithEmptyMessage_ExpectsEmptyMessage() {
		String encryption = EncryptionUtil.encryptPbkdf2("");
		assertEquals("", encryption);
	}

	@Test
	public void testEncryptPbkdf2_WithNullMessage_ExpectsNull() {
		String encryption = EncryptionUtil.encryptPbkdf2(null);
		assertEquals(null, encryption);
	}

	@Test
	public void testEncryptAesCtr_WithValidMessage_ExpectsAesCtrHash() throws Exception {
		String encryption = EncryptionUtil.encryptAESCTR(unEncryptedMessage);
		assertEquals(encryption.substring(32,33), ":");
		assertEquals(encryption.length(), 53);
	}

	@Test
	public void testEncryptAesCtr_WithEmptyMessage_ExpectsEmptyMessage() {
		String encryption = EncryptionUtil.encryptAESCTR("");
		assertEquals("", encryption);
	}

	@Test
	public void testEncryptAesCtr_WithNullMessage_ExpectsNull() {
		String encryption = EncryptionUtil.encryptAESCTR(null);
		assertEquals(null, encryption);
	}

	@Test
	public void testDecryptAesCtr_WithValidMessage_ExpectsDecryptedMessage() {
		String decryption = EncryptionUtil.decryptAESCTR(aesCtrEncryptedMessage);
		assertEquals(unEncryptedMessage, decryption);
	}

	@Test
	public void testDecryptAesCtr_WithNullMessage_ExpectsNull() {
		String decryption = EncryptionUtil.decryptAESCTR(null);
		assertEquals(null, decryption);
	}

	@Test
	public void testDecryptAesCtr_WithEmptyMessage_ExpectsEmptyMessage() {
		String decryption = EncryptionUtil.decryptAESCTR("");
		assertEquals("", decryption);
	}

	@Test
	public void testValidateAesCtr_WithValidMessage_ExpectsSuccess() {
		String encryption = EncryptionUtil.encryptAESCTR(unEncryptedMessage);
		assertTrue(EncryptionUtil.validateAESCTR(unEncryptedMessage, encryption));
	}

	@Test
	public void testEncryptAESGCM_withValidInputParameters_encryptionSucceeds() throws EncryptionException {
		// Arrange.
		// (in setUp() method)
		
		// Act.
		byte[] encrypted = EncryptionUtil.encryptAESGCM(plainText, key, iv, aad);
		
		// Assert.
		assertEquals(plainText.length + 16 /* 128 bits for AES GCM tag */, encrypted.length);
		assertFalse(Arrays.equals(plainText, Arrays.copyOfRange(encrypted, 0, plainText.length)));
	}
	
	@Test
	public void testEncryptAESGCM_withNullPlainText_throwsEncryptionException() throws EncryptionException {
		// Arrange.
		byte[] plainText = null;
		
		// Assert.
		exception.expect(EncryptionException.class);
		
		// Act.
		EncryptionUtil.encryptAESGCM(plainText, key, iv, aad);
	}
	
	@Test
	public void testEncryptAESGCM_withEmptyPlainText_encryptionSucceeds() throws EncryptionException {
		// Arrange.
		byte[] plainText = new byte[0];
		
		// Act.
		byte[] encrypted = EncryptionUtil.encryptAESGCM(plainText, key, iv, aad);
		
		// Assert.
		assertEquals(16 /* 128 bits for AES GCM tag */, encrypted.length);
	}
	
	@Test
	public void testEncryptAESGCM_withNullKey_throwsEncryptionException() throws EncryptionException {
		// Arrange.
		SecretKey key = null;
		
		// Assert.
		exception.expect(EncryptionException.class);
		
		// Act.
		EncryptionUtil.encryptAESGCM(plainText, key, iv, aad);
	}
	
	@Test
	public void testEncryptAESGCM_withNullIV_throwsEncryptionException() throws EncryptionException {
		// Arrange.
		byte[] iv = null;
		
		// Assert.
		exception.expect(EncryptionException.class);
		
		// Act.
		EncryptionUtil.encryptAESGCM(plainText, key, iv, aad);
	}
	
	@Test
	public void testEncryptAESGCM_withInvalidIVLength_throwsEncryptionException() throws EncryptionException {
		// Arrange.
		byte[] iv = "123456789".getBytes(); // length is 9, should be 16
		
		// Assert.
		exception.expect(EncryptionException.class);
		
		// Act.
		EncryptionUtil.encryptAESGCM(plainText, key, iv, aad);
	}

	@Test
	public void testDecryptAESGCM_withValidInputParameters_decryptsCipherTextProperly() throws EncryptionException {
		// Arrange.
		byte[] encrypted = EncryptionUtil.encryptAESGCM(plainText, key, iv, aad);
		
		// Assert.
		byte[] decrypted = EncryptionUtil.decryptAESGCM(encrypted, key, iv, aad);
		
		// Act.
		assertArrayEquals(plainText, decrypted);
	}
	
	@Test
	public void testDecryptAESGCM_withInvalidIV_throwsEncryptionException() throws EncryptionException {
		// Arrange.
		byte[] invalidIv = "SomeInvalidIv123".getBytes();
		byte[] encrypted = EncryptionUtil.encryptAESGCM(plainText, key, iv, aad);
		
		// Assert.
		exception.expect(EncryptionException.class);
				
		// Act.
		EncryptionUtil.decryptAESGCM(encrypted, key, invalidIv, aad);
	}
	
	@Test
	public void testDecryptAESGCM_withInvalidKey_throwsEncryptionException() throws EncryptionException {
		// Arrange.
		byte[] encrypted = EncryptionUtil.encryptAESGCM(plainText, key, iv, aad);
		
		// Assert.
		exception.expect(EncryptionException.class);
				
		// Act.
		EncryptionUtil.decryptAESGCM(encrypted, invalidKey, iv, aad);
	}
	
	@Test
	public void testDecryptAESGCM_withInvalidAAD_throwsEncryptionException() throws EncryptionException {
		// Arrange.
		byte[] invalidAAD = "Some invalid AAD".getBytes();
		byte[] encrypted = EncryptionUtil.encryptAESGCM(plainText, key, iv, aad);
		
		// Assert.
		exception.expect(EncryptionException.class);
				
		// Act.
		EncryptionUtil.decryptAESGCM(encrypted, invalidKey, iv, invalidAAD);
	}
	
    private SecretKey generateTestAESGCMKey() {
		try {
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(128, SecureRandom.getInstance("SHA1PRNG"));
			return keyGen.generateKey();
		} catch (NoSuchAlgorithmException e) {
			fail(e.getMessage());
		}
		
		return null;
	}
}
