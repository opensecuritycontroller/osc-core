package org.osc.core.util.encryption;

import org.apache.commons.lang.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * Class that decrypts DES message
 * @deprecated use only for migration to non-deprecated methods purposes
 */
@Deprecated
public class DESDecryption {
    public String decrypt(String cipherText) throws EncryptionException {
        if(StringUtils.isBlank(cipherText)) {
            return cipherText;
        }

        try {
            return fromEncryptedBytes( Base64.getDecoder().decode(cipherText) );
        } catch(Exception e) {
            throw new EncryptionException("Failed to decrypt cipher text with 3DES", e);
        }
    }

    private String fromEncryptedBytes(byte[] message) throws Exception {

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
}
