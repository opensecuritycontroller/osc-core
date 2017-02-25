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

import org.osc.core.util.encryption.AESCTREncryption;
import org.osc.core.util.encryption.AESGCMEncryption;
import org.osc.core.util.encryption.DESDecryption;
import org.osc.core.util.encryption.EncryptionException;
import org.osc.core.util.encryption.PBKDF2Derivation;

import javax.crypto.SecretKey;

public class EncryptionUtil {
    public static final String SECURITY_PROPS_RESOURCE_PATH = "/org/osc/core/util/security.properties";
    /**
     * Encrypts plain text with AES-GCM authenticated encryption (details in RFC5084)
     * @param plainText text to be encrypted
     * @param key AES key
     * @param iv initialization vector
     * @param aad additional authentication data
     * @return encrypted AES-GCM data
     */
    public static byte[] encryptAESGCM(byte[] plainText, SecretKey key, byte[] iv, byte[] aad) throws EncryptionException {
        return new AESGCMEncryption().encrypt(plainText, key, iv, aad);
    }

    /**
     * Decrypts cipher text with AES-GCM authenticated encryption (details in RFC5084)
     * @param cipherText encrypted text
     * @param key AES key
     * @param iv initialization vector
     * @param aad additional authentication data
     * @return decrypted AES-GCM data
     */
    public static byte[] decryptAESGCM(byte[] cipherText, SecretKey key, byte[] iv, byte[] aad) throws EncryptionException {
        return new AESGCMEncryption().decrypt(cipherText, key, iv, aad);
    }

    /**
     * Encrypt plain text with AES-CTR (counter mode) (details in RFC3686)
     * AES key is loaded from keystore
     * @param plainText text to be encrypted
     * @return IV and cypher text concatenated with ':' character
     */
    public static String encryptAESCTR(String plainText) throws EncryptionException {
        return new AESCTREncryption().encrypt(plainText);
    }


    /**
     * Decrypts cypher text with AES-CTR (counter mode) (details in RFC3686)
     * AES key is loaded from keystore
     * @param cipherText concatenation of IV and cipher text to be decrypted
     * @return decrypted plain text
     */
    public static String decryptAESCTR(String cipherText) throws EncryptionException {
        return new AESCTREncryption().decrypt(cipherText);
    }

    /**
     * Checks if given cipher text is AES-CTR-encrypted version of given plain text
     * @param plainText plain text to be checked
     * @param validCipherText IV and cypher text concatenated with ':' character
     * @return true if given cipher text is encrypted version of given plain text, false otherwise
     */
    public static boolean validateAESCTR(String plainText, String validCipherText) throws EncryptionException {
        return new AESCTREncryption().validate(plainText, validCipherText);
    }

    /**
     * Encrypt plain text with PBKDF2 key derivation algorithm (details in RFC6070)
     * @param plainText plain text to be encrypted
     * @return encrypted (derived) version
     */
    static String encryptPbkdf2(String plainText) throws EncryptionException {
        return new PBKDF2Derivation().derive(plainText);
    }

    /**
     * Verifies if the cipher text is derived version of given plain text
     * @param plainText plain text to check
     * @param validCipherText derived version of some plain text
     * @return true if the cipher text is derived version of given plain text, false otherwise
     */
    static boolean validatePbkdf2(String plainText, String validCipherText) throws EncryptionException {
        return new PBKDF2Derivation().validate(plainText, validCipherText);
    }

    /**
     * Decrypt DES message
     * @deprecated use only for migration to non-deprecated methods purposes
     * @param cipherText encoded string
     * @return decoded string
     */
    @Deprecated
    public static String decryptDES(String cipherText) throws EncryptionException {
        return new DESDecryption().decrypt(cipherText);
    }
}
