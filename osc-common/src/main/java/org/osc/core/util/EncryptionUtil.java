package org.osc.core.util;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;


public class EncryptionUtil {
    private static final Logger log = Logger.getLogger(EncryptionUtil.class);

    // INNER TYPES
    public static class EncryptionException extends Exception {
        private static final long serialVersionUID = -52733376278542276L;

        public EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }

        public EncryptionException(String message) {
            super(message);
        }
    }

    private static byte[] toEncryptedBytes(String message) throws Exception {
        final MessageDigest md = MessageDigest.getInstance("md5");
        final byte[] digestOfPassword = md.digest("HG58YZ3CR9".getBytes("utf-8"));
        final byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);
        for (int j = 0, k = 16; j < 8;) {
            keyBytes[k++] = keyBytes[j++];
        }

        final SecretKey key = new SecretKeySpec(keyBytes, "DESede");
        final IvParameterSpec iv = new IvParameterSpec(new byte[8]);
        final Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);

        final byte[] plainTextBytes = message.getBytes("utf-8");
        final byte[] cipherText = cipher.doFinal(plainTextBytes);

        return cipherText;
    }

    public static String encrypt(String message) {

        if (message == null || message.isEmpty()) {
            return message;
        }

        try {

            byte[] cipherText = toEncryptedBytes(message);
            return new String(Base64.getEncoder().encode(cipherText), "UTF-8");
        } catch (Exception ex) {
            log.error("Error encrypting message", ex);
        }

        return message;

    }

    private static String fromEncryptedBytes(byte[] message) throws Exception {

        final MessageDigest md = MessageDigest.getInstance("md5");
        final byte[] digestOfPassword = md.digest("HG58YZ3CR9".getBytes("UTF-8"));
        final byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);
        for (int j = 0, k = 16; j < 8;) {
            keyBytes[k++] = keyBytes[j++];
        }

        final SecretKey key = new SecretKeySpec(keyBytes, "DESede");
        final IvParameterSpec iv = new IvParameterSpec(new byte[8]);
        final Cipher decipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        decipher.init(Cipher.DECRYPT_MODE, key, iv);

        final byte[] plainText = decipher.doFinal(message);

        return new String(plainText, "UTF-8");
    }

    public static String decrypt(String encodedString) {

        if (encodedString == null || encodedString.isEmpty()) {
            return encodedString;
        }

        try {
            return fromEncryptedBytes(Base64.getDecoder().decode(encodedString));
        } catch (Exception ex) {
            log.error("Error decrypting string", ex);
        }

        return encodedString;
    }

    /**
     * Encrypts plain text with AES-GCM authenticated encryption (details in RFC5084)
     * @param plainText text to be encrypted
     * @param key AES key
     * @param iv initialization vector
     * @param aad additional authentication data
     * @return
     */
    public static byte[] encryptAESGCM(byte[] plainText, SecretKey key, byte[] iv, byte[] aad) throws EncryptionException{
        return processAESGCM(Cipher.ENCRYPT_MODE, plainText, key, iv, aad);
    }

    /**
     * Decrypts cipher text with AES-GCM authenticated encryption (details in RFC5084)
     * @param cipherText encrypted text
     * @param key AES key
     * @param iv initialization vector
     * @param aad additional authentication data
     * @return
     */
    public static byte[] decryptAESGCM(byte[] cipherText, SecretKey key, byte[] iv, byte[] aad) throws EncryptionException{
        return processAESGCM(Cipher.DECRYPT_MODE, cipherText, key, iv, aad);
    }

    private static byte[] processAESGCM(int optMode, byte[] plainText, SecretKey key, byte[] iv, byte[] aad) throws EncryptionException{
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(iv.length * 8, iv);
            cipher.init(optMode, key, spec);
            cipher.updateAAD(aad);
            return cipher.doFinal(plainText);
        } catch (NoSuchAlgorithmException e) {
            String msg = "AES/GCM/NoPadding algorithm is not supported.";
            log.error(msg, e);
            throw new EncryptionException(msg, e);
        } catch (NoSuchPaddingException e) {
            String msg = "Failed to create cipher.";
            log.error(msg, e);
            throw new EncryptionException(msg, e);
        } catch (InvalidKeyException e) {
            String msg = "The key used for AES GCM encryption is invalid.";
            log.error(msg, e);
            throw new EncryptionException(msg, e);
        } catch (InvalidAlgorithmParameterException e) {
            String msg = "IV used for AES GCM encryption is invalid.";
            log.error(msg, e);
            throw new EncryptionException(msg, e);
        } catch (IllegalBlockSizeException e) {
            String msg = "Failed to perform encryption (i.e. data length doesn't match block size).";
            log.error(msg, e);
            throw new EncryptionException(msg, e);
        } catch (AEADBadTagException e) {
            String msg = "Failed to perform encryption. Additional password is incorrect.";
            log.error(msg, e);
            throw new EncryptionException(msg, e);
        } catch (BadPaddingException e) {
            String msg = "Failed to perform encryption. Plain text is not padded properly.";
            log.error(msg, e);
            throw new EncryptionException(msg, e);
        } catch (Exception e) {
            String msg = "Failed to perform encryption.";
            log.error(msg, e);
            throw new EncryptionException(msg, e);
        }
    }
}
