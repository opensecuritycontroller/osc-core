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

public class PBKDF2Derivation {
    private static final Logger LOG = Logger.getLogger(AESCTREncryption.class);
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final int PBKDF2_KEY_LENGTH = 24 * 8;
    private static final int PBKDF2_ITERATIONS = 4000;
    private static final int SALT_BYTES = 24;

    private static final int SALT_INDEX = 1;
    private static final int PBKDF2_INDEX = 2;

    public String derive(String plainText) {
        if (StringUtils.isNotBlank(plainText)) {
            try {
                char[] passwordChars = plainText.toCharArray();
                byte[] saltBytes = getSalt();
                byte[] hashedPasswordBytes = pbkdf2(passwordChars, saltBytes);

                plainText = "";
                for (int i=0; i < passwordChars.length; i++) {
                    passwordChars[i] = 0;
                }

                return PBKDF2_ITERATIONS + ":" +
                        DatatypeConverter.printHexBinary(saltBytes) + ":" +
                        DatatypeConverter.printHexBinary(hashedPasswordBytes);
            } catch (Exception ex) {
                LOG.error("Error encrypting plainText", ex);
            }
        }

        return plainText;
    }

    public boolean validate(String plainText, String validCipherText) {
        if (StringUtils.isNotBlank(plainText)) {
            try {
                String[] params = validCipherText.split(":");
                byte[] salt = DatatypeConverter.parseHexBinary(params[SALT_INDEX]);
                byte[] hash = DatatypeConverter.parseHexBinary(params[PBKDF2_INDEX]);

                byte[] testHash = pbkdf2(plainText.toCharArray(), salt);

                return ByteOperations.slowEquals(hash, testHash);
            } catch (Exception ex) {
                LOG.error("Error validation plainText", ex);
            }
        }
        return false;
    }

    private byte[] pbkdf2(char[] passwordChars, byte[] saltBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec pbeKeySpec = new PBEKeySpec(passwordChars, saltBytes, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH);
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