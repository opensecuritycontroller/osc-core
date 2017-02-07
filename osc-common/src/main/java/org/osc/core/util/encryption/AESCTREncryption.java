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
import java.util.Properties;

public class AESCTREncryption {
    private static final Logger LOG = Logger.getLogger(AESCTREncryption.class);
    private static final int IV_BYTES = 16;
    private static final String AESCTR_ALGORITHM = "AES/CTR/PKCS5Padding";
    private static final int IV_INDEX = 0;
    private static final int AES_INDEX = 1;

    public static final String PROPS_AESCTR_PASSWORD = "aesctr.password";

    public String encrypt(String plainText) {
        if (StringUtils.isNotBlank(plainText)) {
            byte[] plainTextBytes = plainText.getBytes();
            byte[] iv = generateIv();

            return DatatypeConverter.printHexBinary(iv) + ":" +
                    DatatypeConverter.printHexBinary(encryptAesCtr(plainTextBytes, iv));
        }
        return plainText;
    }

    public String decrypt(String cipherText) {
        if (StringUtils.isNotBlank(cipherText)) {
            try {
                String[] params = cipherText.split(":");
                byte[] iv = DatatypeConverter.parseHexBinary(params[IV_INDEX]);
                byte[] hash = DatatypeConverter.parseHexBinary(params[AES_INDEX]);

                SecretKey key = new SecretKeySpec(DatatypeConverter.parseHexBinary(getHexKeyFromKeyStore()), "AES");
                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                Cipher cipher = Cipher.getInstance(AESCTR_ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
                byte[] result = cipher.doFinal(DatatypeConverter.parseHexBinary(DatatypeConverter.printHexBinary(hash)));

                return new String(result, "UTF-8");
            } catch (Exception ex) {
                LOG.error("Error encrypting message", ex);
            }
        }

        return cipherText;
    }

    public boolean validate(String plainText, String validCipherText) {
        if (StringUtils.isNotBlank(plainText)) {
            try {
                byte[] passwordBytes = plainText.getBytes();
                String[] params = validCipherText.split(":");
                byte[] iv = DatatypeConverter.parseHexBinary(params[IV_INDEX]);
                byte[] hash = DatatypeConverter.parseHexBinary(params[AES_INDEX]);

                byte[] testHash = encryptAesCtr(passwordBytes, iv);

                return ByteOperations.slowEquals(hash, testHash);
            } catch (Exception ex) {
                LOG.error("Error validation plainText", ex);
            }
        }
        return false;
    }

    private byte[] encryptAesCtr(byte[] passwordBytes, byte[] iv) {
        try {
            SecretKey key = new SecretKeySpec(DatatypeConverter.parseHexBinary(getHexKeyFromKeyStore()), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance(AESCTR_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            byte[] result = cipher.doFinal(DatatypeConverter.parseHexBinary(DatatypeConverter.printHexBinary(passwordBytes)));

            for (int i=0; i < passwordBytes.length; i++) {
                passwordBytes[i] = 0;
            }

            return result;
        } catch (Exception ex) {
            LOG.error("Error encrypting plainText", ex);
        }
        return passwordBytes;
    }

    private byte[] generateIv() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);
        return iv;
    }

    private String getHexKeyFromKeyStore() {
        String hexKey = null;
        try {
            String aesCtrPassword = loadKeystorePasswordForAESCTRKey();

            if(StringUtils.isBlank(aesCtrPassword)) {
                throw new Exception("Keystore password not found in security properties file");
            }

            KeyStoreProvider keyStoreProvider = KeyStoreProvider.getInstance();
            hexKey = keyStoreProvider.getPassword("AesCtrKey", aesCtrPassword);
            if (hexKey == null) {
                hexKey = DatatypeConverter.printHexBinary(KeyGenerator.getInstance("AES").generateKey().getEncoded());
                keyStoreProvider.putPassword("AesCtrKey", hexKey, aesCtrPassword);
            }
        } catch (Exception e) {
            LOG.error("Error encrypting plainText", e);
        }

        return hexKey;
    }

    private String loadKeystorePasswordForAESCTRKey() {
        Properties properties = new Properties();
        try {
            properties.load(getClass().getResourceAsStream(EncryptionUtil.SECURITY_PROPS_RESOURCE_PATH));
        } catch (IOException e) {
            LOG.error("Error loading key from properties", e);
        }
        return properties.getProperty(PROPS_AESCTR_PASSWORD);
    }
}
