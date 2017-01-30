package org.osc.core.util;

import static org.junit.Assert.*;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;


import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.EncryptionUtil.EncryptionException;

public class EncryptionUtilTest {

	private String unEncryptedMessage = "helloworld";
	private String encryptedMessage = "SvQhNtPMXaAV85ot1FkogA==";

	private SecretKey key = generateTestAESGCMKey();
	private SecretKey invalidKey = generateTestAESGCMKey();
	private byte[] plainText;
	private byte[] iv;
	private byte[] aad;
	
	@Before
	public void setUp() throws NoSuchAlgorithmException {
		// Arrange.
		plainText = "Some text to encrypt".getBytes();
		iv = new byte[16];
		aad = "Some additional authentication data".getBytes();
		new SecureRandom().nextBytes(iv);
	}
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	/** To check the valid behavior of encryption with valid string message */
	@Test
	public void testEncryption() {
		String encryption = EncryptionUtil.encrypt(unEncryptedMessage);
		assertEquals(encryptedMessage, encryption);
	}

	/** To check the valid behavior of decryption with valid encoded string */
	@Test
	public void testDecryption() {
		String decryption = EncryptionUtil.decrypt(encryptedMessage);
		assertEquals(unEncryptedMessage, decryption);
	}

	/** To check the valid behavior of encryption with null message */
	@Test
	public void testNullMessageEncryption() {
		String encryption = EncryptionUtil.encrypt(null);
		assertEquals(null, encryption);
	}

	/** To check the valid behavior of decryption with null encoded string */
	@Test
	public void testNullMessageDecryption() {
		String decryption = EncryptionUtil.decrypt(null);
		assertEquals(null, decryption);
	}

	/** To check the valid behavior of encryption with empty message */
	@Test
	public void testEmptyMessageEncrytion() {
		String encryption = EncryptionUtil.encrypt("");
		assertEquals("", encryption);
	}

	/** To check the valid behavior of decryption with empty encoded string */
	@Test
	public void testEmptyMessageDecryption() {
		String decryption = EncryptionUtil.decrypt("");
		assertEquals("", decryption);
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
