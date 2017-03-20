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

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public class PBKDF2Derivation {
    private static final Logger LOG = Logger.getLogger(AESCTREncryption.class);
    private static final int PBKDF2_ITERATIONS = 4000;
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final int PBKDF2_KEY_LENGTH = 24 * 8;

    private static final int SALT_BYTES = 24;
    private static final int ITERATIONS_INDEX = 0;
    private static final int SALT_INDEX = 1;
    private static final int PBKDF2_INDEX = 2;

    public String derive(String plainText) throws EncryptionException {
        if (StringUtils.isBlank(plainText)) {
            return plainText;
        }

        try {
            char[] passwordChars = plainText.toCharArray();
            byte[] saltBytes = getSalt();
            byte[] hashedPasswordBytes = pbkdf2(passwordChars, saltBytes, PBKDF2_ITERATIONS);

            plainText = "";

            Arrays.fill(passwordChars, (char) 0);

            return String.join(":", Integer.toString(PBKDF2_ITERATIONS),
                                    DatatypeConverter.printHexBinary(saltBytes),
                                    DatatypeConverter.printHexBinary(hashedPasswordBytes));
        } catch (Exception ex) {
            String msg = "Error during PBKDF2 derivation";
            LOG.error(msg, ex);
            throw new EncryptionException(msg, ex);
        }

    }

    public boolean validate(String plainText, String validCipherText) throws EncryptionException {
        if (StringUtils.isBlank(plainText)) {
            return false;
        }

        try {
            String[] params = validCipherText.split(":");
            int iterations = DatatypeConverter.parseInt(params[ITERATIONS_INDEX]);
            byte[] salt = DatatypeConverter.parseHexBinary(params[SALT_INDEX]);
            byte[] hash = DatatypeConverter.parseHexBinary(params[PBKDF2_INDEX]);

            byte[] testHash = pbkdf2(plainText.toCharArray(), salt, iterations);

            return ByteOperations.slowEquals(hash, testHash);
        } catch (Exception ex) {
            String msg = "Error during validation of PBKDF2 derived cypher";
            LOG.error(msg, ex);
            throw new EncryptionException(msg, ex);
        }
    }

    private byte[] pbkdf2(char[] passwordChars, byte[] saltBytes, int pbkdf2Iterations) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec pbeKeySpec = new PBEKeySpec(passwordChars, saltBytes, pbkdf2Iterations, PBKDF2_KEY_LENGTH);
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        SecretKey secretKey = secretKeyFactory.generateSecret(pbeKeySpec);
        pbeKeySpec.clearPassword();
        return secretKey.getEncoded();
    }

    private byte[] getSalt() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        return salt;
    }
}