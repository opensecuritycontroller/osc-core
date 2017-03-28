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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class PKIUtil {

    private static final Logger log = Logger.getLogger(PKIUtil.class);

    private static final String keyAlg = "RSA";
    private static final String dName = "CN=Intruvert-Sensor,C=US";
    private static final String keySize = "2048";
    private static final String validity = "999999";
    private static final String keyPass = "abc12345";
    private static final String storePass = keyPass;
    private static final String sigAlg = "SHA256withRSA";
    private static final String alias = "vsensorKS";

    public static byte[] generateMD5Checksum(String fileName) {

        InputStream fis = null;

        try {
            fis = new FileInputStream(fileName);

            byte[] buffer = new byte[1024 * 4];
            MessageDigest checksum = MessageDigest.getInstance("MD5");
            int numRead;

            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    checksum.update(buffer, 0, numRead);
                }
            } while (numRead != -1);

            return checksum.digest();

        } catch (Exception ex) {

            log.error("failed to generate MD5 checksum", ex);

        } finally {
            IOUtils.closeQuietly(fis);
        }

        return null;
    }

    public static String toBase64EncodedString(byte[] bytes) throws Exception {
        return new String(Base64.getEncoder().encode(bytes));
    }

    private static int generateKeyStoreFile(String keyStoreFile) {

        String javaHome = System.getProperty("java.home");
        File keytoolExecutable = new File(javaHome);
        keytoolExecutable = new File(keytoolExecutable, "bin");
        keytoolExecutable = new File(keytoolExecutable, "keytool");
        String keytoolExecutablePath = keytoolExecutable.getAbsolutePath();

        log.info("Launching keytool");

        String[] command = new String[] { keytoolExecutablePath, "-genkey", "-alias", alias, "-keypass", keyPass,
                "-dname", dName, "-keyalg", keyAlg, "-keysize", keySize, "-validity", validity, "-keystore",
                keyStoreFile, "-storepass", storePass, "-sigalg", sigAlg };

        return ServerUtil.execWithLog(command, false);
    }

    public static void writeInputStreamToFile(InputStream is, String parentFolderName, String fileName) {

        StringBuilder sb = new StringBuilder(parentFolderName)
                .append(File.separator)
                .append(fileName);

        Path file = Paths.get(sb.toString());
        Path backup = Paths.get(sb.append(".org").toString());

        log.info("Start writing input stream to file: " + file);

        if(StringUtils.equals(String.valueOf(file.getFileName()),fileName)) {
            try {
                if (Files.exists(file)) {
                    log.info("Renaming/backup existing file");
                    Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
                }
                try {
                    Files.copy(is, file);
                    log.info("Successfully wrote input stream to file '" + file + "'");
                } catch (Exception ex) {
                    log.error("Failed to write input stream to file", ex);
                    // undo when fails
                    if (Files.exists(backup)) {
                        Files.move(backup, file, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                Files.deleteIfExists(backup);
            } catch (Exception e) {
                log.error("Failed to write input stream to file" + file, e);
            }
        } else {
            log.warn("Filename: " + fileName + " is not valid");
        }
    }

    public static void writeBytesToFile(byte[] bytes, String parentFolderName, String fileName) {

        StringBuilder sb = new StringBuilder(parentFolderName)
                .append(File.separator)
                .append(fileName);

        Path file = Paths.get(sb.toString());
        Path backup = Paths.get(sb.append(".org").toString());

        log.info("Start writing " + bytes.length + " bytes to file " + file);

        if(StringUtils.equals(String.valueOf(file.getFileName()),fileName)) {
            try {
                if (Files.exists(file)) {
                    log.info("Renaming/backup existing file");
                    Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
                }
                try {
                    Files.write(file, bytes);
                    log.info("Successfully wrote " + bytes.length + " bytes to file '" + file + "'");
                } catch (Exception ex) {
                    log.error("Failed to convert bytes to file", ex);
                    // undo when fails
                    if (Files.exists(backup)) {
                        Files.move(backup, file, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                Files.deleteIfExists(backup);
            } catch (Exception e) {
                log.error("Failed to write bytes to file" + file, e);
            }
        } else {
            log.warn("Filename: " + fileName + " is not valid");
        }
    }

    public static byte[] readBytesFromFile(File file) {

        if (!file.exists()) {
            return null;
        }

        FileInputStream fis = null;
        ByteArrayOutputStream bos = null;

        try {

            bos = new ByteArrayOutputStream();
            fis = new FileInputStream(file);

            int nRead;
            byte[] data = new byte[1024 * 4];

            while ((nRead = fis.read(data, 0, data.length)) != -1) {
                bos.write(data, 0, nRead);
            }

            bos.flush();

            return bos.toByteArray();

        } catch (Exception ex) {
            log.error("failed to convert file to byte array", ex);

        } finally {

            IOUtils.closeQuietly(fis);
            IOUtils.closeQuietly(bos);
        }

        return null;
    }

    public static byte[] generateKeyStore() throws Exception {
        String tempFile = null; // fully-qualified file name

        try {
            FileUtils.forceMkdir(new File("tmp"));

            tempFile = "./tmp/vmidcKs-" + new Date().getTime() + ".jks";
            int exitCode = generateKeyStoreFile(tempFile);

            if (exitCode != 0) {
                throw new Exception("Failed generate key. Error code: " + exitCode);
            }
            byte [] bytes = readBytesFromFile(new File(tempFile));
            if (bytes == null || bytes.length == 0) {
                throw new Exception("Failed generate key.");
            }

            // convert the newly-generated keystore file to byte array
            return bytes;

        } finally {

            if (tempFile != null) {
                try {
                    new File(tempFile).delete();
                } catch (Exception e) {
                    log.error("generateKeyStore(): Failed to delete temp file", e);
                }
            }
        }
    }

    public static boolean verifyKeyPair(byte[] keyStoreBytes, PublicKey pubKey) {

        try {
            getCertificate(keyStoreBytes).verify(pubKey);

        } catch (Exception ex) {

            log.error("Failed to verify public key", ex);
            return false;
        }

        return true;
    }

    private static Certificate getCertificate(byte[] keyStoreBytes) throws Exception {
        try {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(new ByteArrayInputStream(keyStoreBytes), keyPass.toCharArray());
            return keystore.getCertificate(alias);
        }catch (Exception e) {
            throw new Exception("Failed to get certificate from key store");
        }
    }

    public static PublicKey getPubKey(byte[] keyStoreBytes) throws Exception {

        return getCertificate(keyStoreBytes).getPublicKey();
    }

    public static byte[] extractCertificate(byte[] keyStoreBytes) throws Exception {

        return getCertificate(keyStoreBytes).getEncoded();
    }

    public static byte[] extractPrivateKey(byte[] keyStoreBytes) throws Exception {

        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(new ByteArrayInputStream(keyStoreBytes), keyPass.toCharArray());
        Key key = keystore.getKey(alias, keyPass.toCharArray());

        if (key instanceof PrivateKey) {

            return key.getEncoded();
        }

        throw new Exception("Private key not found in key store.");
    }
}
