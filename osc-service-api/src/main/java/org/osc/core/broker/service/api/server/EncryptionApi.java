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
package org.osc.core.broker.service.api.server;

import javax.crypto.SecretKey;

public interface EncryptionApi {

    /**
     * Encrypts plain text with AES-GCM authenticated encryption (details in RFC5084)
     * @param plainText text to be encrypted
     * @param key AES key
     * @param iv initialization vector
     * @param aad additional authentication data
     * @return encrypted AES-GCM data
     */
    byte[] encryptAESGCM(byte[] plainText, SecretKey key, byte[] iv, byte[] aad) throws EncryptionException;

    /**
     * Decrypts cipher text with AES-GCM authenticated encryption (details in RFC5084)
     * @param cipherText encrypted text
     * @param key AES key
     * @param iv initialization vector
     * @param aad additional authentication data
     * @return decrypted AES-GCM data
     */
    byte[] decryptAESGCM(byte[] cipherText, SecretKey key, byte[] iv, byte[] aad) throws EncryptionException;

    /**
     * Encrypt plain text with AES-CTR (counter mode) (details in RFC3686)
     * AES key is loaded from keystore
     * @param plainText text to be encrypted
     * @return IV and cypher text concatenated with ':' character
     */
    String encryptAESCTR(String plainText) throws EncryptionException;


    /**
     * Decrypts cypher text with AES-CTR (counter mode) (details in RFC3686)
     * AES key is loaded from keystore
     * @param cipherText concatenation of IV and cipher text to be decrypted
     * @return decrypted plain text
     */
    String decryptAESCTR(String cipherText) throws EncryptionException;

    /**
     * Checks if given cipher text is AES-CTR-encrypted version of given plain text
     * @param plainText plain text to be checked
     * @param validCipherText IV and cypher text concatenated with ':' character
     * @return true if given cipher text is encrypted version of given plain text, false otherwise
     */
    boolean validateAESCTR(String plainText, String validCipherText) throws EncryptionException;

    /**
     * Decrypt DES message
     * @deprecated use only for migration to non-deprecated methods purposes
     * @param cipherText encoded string
     * @return decoded string
     */
    @Deprecated
    String decryptDES(String cipherText) throws EncryptionException;
}
