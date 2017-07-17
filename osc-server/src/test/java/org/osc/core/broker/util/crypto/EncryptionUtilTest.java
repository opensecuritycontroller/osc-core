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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.osc.core.broker.service.api.server.EncryptionException;

public class EncryptionUtilTest {
    // AES-CTR test data
    private String unEncryptedMessage = "helloworld";
    private String aesCtrEncryptedMessage = "af5b59f52f5c3f0a77c6ba3bae08c1fe:26255b1fabfecfc469af";

    // AES-GCM test data
    private SecretKey key = generateTestAESGCMKey();
    private SecretKey invalidKey = generateTestAESGCMKey();
    private byte[] plainText;
    private byte[] iv;
    private byte[] aad;

    @Before
    public void setUp() throws Exception {
        // Arrange.
        this.plainText = "Some text to encrypt".getBytes();
        this.iv = new byte[16];
        this.aad = "Some additional authentication data".getBytes();
        new SecureRandom().nextBytes(this.iv);

        AESCTREncryption.setKeyProvider(new AESCTREncryption.KeyProvider() {
            @Override
            public String getKeyHex() throws EncryptionException {
                return "1234567890abcdef1234567890abcdef";
            }

            @Override
            public void updateKey(String keyHex) throws EncryptionException {
                // dont do nothing
            }
        });

    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    /** To check the valid behavior of encryption with valid string message */
    @Test
    public void testEncryptPbkdf2_WithValidMessage_ExpectsPbkdf2Hash() throws EncryptionException {
        String encryption = EncryptionUtil.encryptPbkdf2(this.unEncryptedMessage);
        assertTrue(encryption.startsWith("4000"));
        assertEquals(encryption.length(), 102);
    }

    @Test
    public void testValidatePbkdf2_WithValidMessage_ExpectsSuccess() throws EncryptionException {
        String encryption = EncryptionUtil.encryptPbkdf2(this.unEncryptedMessage);
        assertTrue(EncryptionUtil.validatePbkdf2(this.unEncryptedMessage, encryption));
    }

    @Test
    public void testEncryptPbkdf2_WithEmptyMessage_ExpectsEmptyMessage() throws EncryptionException {
        String encryption = EncryptionUtil.encryptPbkdf2("");
        assertEquals("", encryption);
    }

    @Test
    public void testEncryptPbkdf2_WithNullMessage_ExpectsNull() throws EncryptionException {
        String encryption = EncryptionUtil.encryptPbkdf2(null);
        assertEquals(null, encryption);
    }

    @Test
    public void testEncryptAesCtr_WithValidMessage_ExpectsAesCtrHash() throws Exception {
        String encryption = new EncryptionUtil().encryptAESCTR(this.unEncryptedMessage);
        assertEquals(encryption.substring(32,33), ":");
        assertEquals(encryption.length(), 53);
    }

    @Test
    public void testEncryptAesCtr_WithEmptyMessage_ExpectsEmptyMessage() throws EncryptionException {
        String encryption = new EncryptionUtil().encryptAESCTR("");
        assertEquals("", encryption);
    }

    @Test
    public void testEncryptAesCtr_WithNullMessage_ExpectsNull() throws EncryptionException {
        String encryption = new EncryptionUtil().encryptAESCTR(null);
        assertEquals(null, encryption);
    }

    @Test
    public void testDecryptAesCtr_WithValidMessage_ExpectsDecryptedMessage() throws EncryptionException {
        String decryption = new EncryptionUtil().decryptAESCTR(this.aesCtrEncryptedMessage);
        assertEquals(this.unEncryptedMessage, decryption);
    }

    @Test
    public void testDecryptAesCtr_WithNullMessage_ExpectsNull() throws EncryptionException {
        String decryption = new EncryptionUtil().decryptAESCTR(null);
        assertEquals(null, decryption);
    }

    @Test
    public void testDecryptAesCtr_WithEmptyMessage_ExpectsEmptyMessage() throws EncryptionException {
        String decryption = new EncryptionUtil().decryptAESCTR("");
        assertEquals("", decryption);
    }

    @Test
    public void testValidateAesCtr_WithValidMessage_ExpectsSuccess() throws EncryptionException {
        String encryption = new EncryptionUtil().encryptAESCTR(this.unEncryptedMessage);
        assertTrue(new EncryptionUtil().validateAESCTR(this.unEncryptedMessage, encryption));
    }

    @Test
    public void testEncryptAESGCM_withValidInputParameters_encryptionSucceeds() throws EncryptionException {
        // Arrange.
        // (in setUp() method)

        // Act.
        byte[] encrypted = new EncryptionUtil().encryptAESGCM(this.plainText, this.key, this.iv, this.aad);

        // Assert.
        assertEquals(this.plainText.length + 16 /* 128 bits for AES GCM tag */, encrypted.length);
        assertFalse(Arrays.equals(this.plainText, Arrays.copyOfRange(encrypted, 0, this.plainText.length)));
    }

    @Test
    public void testEncryptAESGCM_withNullPlainText_throwsEncryptionException() throws EncryptionException {
        // Arrange.
        byte[] plainText = null;

        // Assert.
        this.exception.expect(EncryptionException.class);

        // Act.
        new EncryptionUtil().encryptAESGCM(plainText, this.key, this.iv, this.aad);
    }

    @Test
    public void testEncryptAESGCM_withEmptyPlainText_encryptionSucceeds() throws EncryptionException {
        // Arrange.
        byte[] plainText = new byte[0];

        // Act.
        byte[] encrypted = new EncryptionUtil().encryptAESGCM(plainText, this.key, this.iv, this.aad);

        // Assert.
        assertEquals(16 /* 128 bits for AES GCM tag */, encrypted.length);
    }

    @Test
    public void testEncryptAESGCM_withNullKey_throwsEncryptionException() throws EncryptionException {
        // Arrange.
        SecretKey key = null;

        // Assert.
        this.exception.expect(EncryptionException.class);

        // Act.
        new EncryptionUtil().encryptAESGCM(this.plainText, key, this.iv, this.aad);
    }

    @Test
    public void testEncryptAESGCM_withNullIV_throwsEncryptionException() throws EncryptionException {
        // Arrange.
        byte[] iv = null;

        // Assert.
        this.exception.expect(EncryptionException.class);

        // Act.
        new EncryptionUtil().encryptAESGCM(this.plainText, this.key, iv, this.aad);
    }

    @Test
    public void testEncryptAESGCM_withInvalidIVLength_throwsEncryptionException() throws EncryptionException {
        // Arrange.
        byte[] iv = "123456789".getBytes(); // length is 9, should be 16

        // Assert.
        this.exception.expect(EncryptionException.class);

        // Act.
        new EncryptionUtil().encryptAESGCM(this.plainText, this.key, iv, this.aad);
    }

    @Test
    public void testDecryptAESGCM_withValidInputParameters_decryptsCipherTextProperly() throws EncryptionException {
        // Arrange.
        byte[] encrypted = new EncryptionUtil().encryptAESGCM(this.plainText, this.key, this.iv, this.aad);

        // Assert.
        byte[] decrypted = new EncryptionUtil().decryptAESGCM(encrypted, this.key, this.iv, this.aad);

        // Act.
        assertArrayEquals(this.plainText, decrypted);
    }

    @Test
    public void testDecryptAESGCM_withInvalidIV_throwsEncryptionException() throws EncryptionException {
        // Arrange.
        byte[] invalidIv = "SomeInvalidIv123".getBytes();
        byte[] encrypted = new EncryptionUtil().encryptAESGCM(this.plainText, this.key, this.iv, this.aad);

        // Assert.
        this.exception.expect(EncryptionException.class);

        // Act.
        new EncryptionUtil().decryptAESGCM(encrypted, this.key, invalidIv, this.aad);
    }

    @Test
    public void testDecryptAESGCM_withInvalidKey_throwsEncryptionException() throws EncryptionException {
        // Arrange.
        byte[] encrypted = new EncryptionUtil().encryptAESGCM(this.plainText, this.key, this.iv, this.aad);

        // Assert.
        this.exception.expect(EncryptionException.class);

        // Act.
        new EncryptionUtil().decryptAESGCM(encrypted, this.invalidKey, this.iv, this.aad);
    }

    @Test
    public void testDecryptAESGCM_withInvalidAAD_throwsEncryptionException() throws EncryptionException {
        // Arrange.
        byte[] invalidAAD = "Some invalid AAD".getBytes();
        byte[] encrypted = new EncryptionUtil().encryptAESGCM(this.plainText, this.key, this.iv, this.aad);

        // Assert.
        this.exception.expect(EncryptionException.class);

        // Act.
        new EncryptionUtil().decryptAESGCM(encrypted, this.invalidKey, this.iv, invalidAAD);
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
