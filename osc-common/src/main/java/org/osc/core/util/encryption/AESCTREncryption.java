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
package org.osc.core.util.encryption;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.KeyStoreProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Properties;

public class AESCTREncryption {
    private static final Logger LOG = Logger.getLogger(AESCTREncryption.class);
    private static final int IV_BYTES = 16;
    private static final String AESCTR_ALGORITHM = "AES/CTR/PKCS5Padding";
    private static final int IV_INDEX = 0;
    private static final int AES_INDEX = 1;

    public static final String PROPS_AESCTR_PASSWORD = "aesctr.password";

    public AESCTREncryption() {
        if (keyProvider == null) {
            setKeyProvider(new KeyFromKeystoreProvider());
        }
    }

    public String encrypt(String plainText) throws EncryptionException {
        if (StringUtils.isBlank(plainText)) {
            return plainText;
        }

        byte[] plainTextBytes = plainText.getBytes();
        byte[] iv = generateIv();

        return String.join(":", DatatypeConverter.printHexBinary(iv),
                                DatatypeConverter.printHexBinary(encryptAesCtr(plainTextBytes, iv)));
    }

    public String decrypt(String cipherText) throws EncryptionException {
        if(StringUtils.isBlank(cipherText)) {
            return cipherText;
        }

        try {
            String[] params = cipherText.split(":");
            byte[] iv = DatatypeConverter.parseHexBinary(params[IV_INDEX]);
            byte[] hash = DatatypeConverter.parseHexBinary(params[AES_INDEX]);

            SecretKey key = new SecretKeySpec(DatatypeConverter.parseHexBinary(keyProvider.getKeyHex()), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance(AESCTR_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
            byte[] result = cipher.doFinal(DatatypeConverter.parseHexBinary(DatatypeConverter.printHexBinary(hash)));

            return new String(result, "UTF-8");
        } catch (Exception ex) {
            LOG.error("Error encrypting message", ex);
            throw new EncryptionException("Failed to decrypt cipher with AES-CTR", ex);
        }
    }

    public boolean validate(String plainText, String validCipherText) throws EncryptionException {
        if (StringUtils.isBlank(plainText)) {
            return false;
        }

        try {
            byte[] passwordBytes = plainText.getBytes();
            String[] params = validCipherText.split(":");
            byte[] iv = DatatypeConverter.parseHexBinary(params[IV_INDEX]);
            byte[] hash = DatatypeConverter.parseHexBinary(params[AES_INDEX]);

            byte[] testHash = encryptAesCtr(passwordBytes, iv);

            return ByteOperations.slowEquals(hash, testHash);
        } catch (Exception ex) {
            LOG.error("Error validation plainText", ex);
            throw new EncryptionException("Failed to validate AES-CTR cipher against valid cipher text", ex);
        }
    }

    private byte[] encryptAesCtr(byte[] passwordBytes, byte[] iv) throws EncryptionException {
        try {
            SecretKey key = new SecretKeySpec(DatatypeConverter.parseHexBinary(keyProvider.getKeyHex()), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance(AESCTR_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            byte[] result = cipher.doFinal(DatatypeConverter.parseHexBinary(DatatypeConverter.printHexBinary(passwordBytes)));

            Arrays.fill(passwordBytes, (byte)0);

            return result;
        } catch (Exception ex) {
            LOG.error("Error encrypting plainText", ex);
            throw new EncryptionException("Failed to encrypt cipher with AES-CTR", ex);
        }
    }

    private byte[] generateIv() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);
        return iv;
    }

    // KEY PROVIDING STRATEGY
    private static KeyProvider keyProvider;

    public static void setKeyProvider(KeyProvider provider){
        keyProvider = provider;
    }

    public interface KeyProvider {
        String getKeyHex() throws EncryptionException;
    }

    private class KeyFromKeystoreProvider implements KeyProvider {
        @Override
        public String getKeyHex() throws EncryptionException{
            String hexKey = null;
            try {
                String aesCtrPassword = loadKeystorePasswordForAESCTRKey();

                if(StringUtils.isBlank(aesCtrPassword)) {
                    throw new Exception("Keystore password not found in security properties file");
                }

                KeyStoreProvider keyStoreProvider = KeyStoreProvider.getInstance();
                hexKey = keyStoreProvider.getPassword("AesCtrKey", aesCtrPassword);

                if (StringUtils.isBlank(hexKey)) {
                    hexKey = DatatypeConverter.printHexBinary(KeyGenerator.getInstance("AES").generateKey().getEncoded());
                    keyStoreProvider.putPassword("AesCtrKey", hexKey, aesCtrPassword);
                }
            } catch (Exception e) {
                LOG.error("Error encrypting plainText", e);
                throw new EncryptionException("Failed to get encryption key", e);
            }

            return hexKey;
        }

        private String loadKeystorePasswordForAESCTRKey() throws EncryptionException {
            Properties properties = new Properties();
            try {
                properties.load(getClass().getResourceAsStream(EncryptionUtil.SECURITY_PROPS_RESOURCE_PATH));
            } catch (IOException e) {
                LOG.error("Error loading key from properties", e);
                throw new EncryptionException("Failed to load keystore password.", e);
            }
            return properties.getProperty(PROPS_AESCTR_PASSWORD);
        }
    }
}
