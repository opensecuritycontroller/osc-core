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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * Singleton facade allowing to add/remove secrets from keystore
 */
public final class KeyStoreProvider {
    // CONSTANTS
    private static final String KEYSTORE_PATH = "data/mainKeyStore.p12";
    private static final String SECURITY_PROPS_RESOURCE_PATH = "/org/osc/core/broker/util/crypto/security.properties";
    private static final String KEYSTORE_PASSWORD_ALIAS = "keystore.password";
    private static final String SECRET_KEY_PASSWORD_ALGORITHM = "PBE";
    // SINGLETON CODE
    private KeyStoreProvider() throws KeyStoreProviderException {  }

    private static volatile KeyStoreProvider instance;

    public static KeyStoreProvider getInstance() throws KeyStoreProviderException{
        if (instance == null) {
            synchronized(KeyStoreProvider.class) {
                if(instance == null) {
                    instance = new KeyStoreProvider();
                    if(factory == null) {
                        factory = instance.new KeyStoreFromFileFactory();
                    }
                    instance.init();
                }
            }
        }

        return instance;
    }

    // KEY STORE INJECTION
    private static KeyStoreFactory factory = null;

    /**
     * Interface of factory method that create key store
     * Allows to replace the default keystore that is loaded from file
     */
    public interface KeyStoreFactory {
        KeyStore createKeyStore() throws KeyStoreProviderException;
        void persist(KeyStore keyStore) throws KeyStoreProviderException;
    }

    /**
     * Allows to inject the key store that is handled by provider.
     * This method has to be called before first .getInstance call
     * If no custom key store factory set - default one that loads
     * keystore from file is used
     * @param factory factory method that creates keystore
     */
    public static void setKeyStoreFactory(KeyStoreFactory factory) {
        KeyStoreProvider.factory = factory;
    }

    private class KeyStoreFromFileFactory implements KeyStoreFactory {

        @Override
        public KeyStore createKeyStore() throws KeyStoreProviderException {
            KeyStore keystore = null;

            LOG.info("Initializing keystore...");
            String keystorePassword = getKeyStorePassword();

            LOG.info("Opening keystore file....");
            try(InputStream keystoreStream = new FileInputStream(KEYSTORE_PATH)) {
                keystore = KeyStore.getInstance("PKCS12");
                LOG.info("Loading keystore from file....");
                keystore.load(keystoreStream, keystorePassword.toCharArray());
            } catch (IOException ioe) {
                throw new KeyStoreProviderException("Failed to obtain keystore from resources.", ioe);
            } catch (KeyStoreException kse) {
                throw new KeyStoreProviderException("Failed to create PKCS#12 keystore object.", kse);
            } catch (NoSuchAlgorithmException nsae) {
                throw new KeyStoreProviderException("Algorithm used to check the integrity of the keystore cannot be found.", nsae);
            } catch (CertificateException ce) {
                throw new KeyStoreProviderException("Certificates in the keystore could not be loaded.", ce);
            }

            return keystore;
        }

        @Override
        public void persist(KeyStore keyStore) throws KeyStoreProviderException {
            try(OutputStream keystoreOutputStream = getKeyStoreOutputStream()) {
                keyStore.store(keystoreOutputStream, getKeyStorePassword().toCharArray());
            } catch (KeyStoreException e) {
                throw new KeyStoreProviderException("Keystore has not been initialized or loaded", e);
            } catch (NoSuchAlgorithmException e) {
                throw new KeyStoreProviderException("The appropriate data integrity algorithm could not be found", e);
            } catch (CertificateException e) {
                throw new KeyStoreProviderException("Some of the certificates included in the keystore data could not be stored", e);
            } catch (IOException e) {
                throw new KeyStoreProviderException("Some I/O problem occured while storing keystore", e);
            }
        }

    }

    // MEMBERS
    private static final Logger LOG = LoggerFactory.getLogger(KeyStoreProvider.class);
    private KeyStore keystore = null;

    // INNER TYPES
    public final class KeyStoreProviderException extends Exception {
        private static final long serialVersionUID = 6520829096189870519L;

        public KeyStoreProviderException(String message, Throwable cause) {
            super(message, cause);
        }

        public KeyStoreProviderException(String message) {
            super(message);
        }
    }

    private void init() throws KeyStoreProviderException {
        LOG.info("Initializing keystore...");
        this.keystore = factory.createKeyStore();
    }

    private String getKeyStorePassword() throws KeyStoreProviderException {
        try(InputStream is = getClass().getResourceAsStream(SECURITY_PROPS_RESOURCE_PATH)) {
            LOG.info("Obtaining keystore password ...");
            Properties properties = new Properties();
            properties.load(is);
            String keystorePassword = properties.getProperty(KEYSTORE_PASSWORD_ALIAS);

            if(StringUtils.isBlank(keystorePassword)) {
                throw new KeyStoreProviderException("Keystore password not found in security properties file");
            }

            return keystorePassword;
        } catch (IOException ioe) {
            throw new KeyStoreProviderException("Failed to open to the security properties file - properties file was not loaded", ioe);
        }
    }

    private OutputStream getKeyStoreOutputStream() throws KeyStoreProviderException {
        File file = new File(KEYSTORE_PATH);
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException fnfe) {
            throw new KeyStoreProviderException("Keystore file not found in resources.", fnfe);

        }
    }

    /**
     * Puts password in keystore under given alias. Secret password is secured with the entry password.
     * The keystore change is persistant.
     * @param alias alias to secret password
     * @param password password to store as secret
     * @param entryPassword password to secret password
     * @throws KeyStoreProviderException
     */
    public void putPassword(String alias, String password, String entryPassword) throws KeyStoreProviderException {
        try {
            LOG.info(String.format("Putting password with alias %s in keystore ...", alias));

            SecretKeyFactory skFactory = SecretKeyFactory.getInstance(SECRET_KEY_PASSWORD_ALGORITHM);
            SecretKey secret = skFactory.generateSecret(new PBEKeySpec(password.toCharArray()));
            this.keystore.setEntry(alias, new KeyStore.SecretKeyEntry(secret),
                    new KeyStore.PasswordProtection(entryPassword.toCharArray()));
            factory.persist(this.keystore);
        } catch (NoSuchAlgorithmException nsae) {
            throw new KeyStoreProviderException("Algorithm used to create PBE secret cannot be found.", nsae);
        } catch (InvalidKeySpecException ikse) {
            throw new KeyStoreProviderException("Invalid key spec used to create PBE secret.", ikse);
        } catch (KeyStoreException kse) {
            throw new KeyStoreProviderException("Failed to put PBE secret to keystore.", kse);
        } catch (Exception e) {
            throw new KeyStoreProviderException("Failed to put PBE secret in keystore", e);
        }
    }

    /**
     * Gets the secret password stored in keystore under given alias.
     * @param alias
     * @param entryPassword entry password to access the secret password stored in keystore
     * @return the secret password or null if secret password does not exists in keystore
     * @throws KeyStoreProviderException
     */
    public String getPassword(String alias, String entryPassword) throws KeyStoreProviderException {
        try {
            LOG.info(String.format("Getting password with alias %s from keystore ...", alias));

            SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRET_KEY_PASSWORD_ALGORITHM);

            Optional<KeyStore.SecretKeyEntry> ske = Optional.fromNullable((KeyStore.SecretKeyEntry) this.keystore.getEntry(alias, new KeyStore.PasswordProtection(entryPassword.toCharArray())));

            if(!ske.isPresent()) {
                return null;
            }

            PBEKeySpec keySpec = (PBEKeySpec)factory.getKeySpec(ske.get().getSecretKey(),PBEKeySpec.class);
            char[] password = keySpec.getPassword();

            if(ArrayUtils.isEmpty(password)) {
                throw new KeyStoreProviderException("Recovered password is blank.");
            }

            return new String(password);
        } catch (NoSuchAlgorithmException nsae) {
            throw new KeyStoreProviderException("Algorithm used to create PBE secret cannot be found.", nsae);
        } catch (UnrecoverableEntryException uee) {
            throw new KeyStoreProviderException("Invalid entry password to recover secret.", uee);
        } catch (KeyStoreException kse) {
            throw new KeyStoreProviderException("Failed to get PBE secret to keystore.", kse);
        } catch (InvalidKeySpecException ikse) {
            throw new KeyStoreProviderException("Failed to get key spec from PBE secret.", ikse);
        } catch (Exception e) {
            throw new KeyStoreProviderException("Failed to get PBE secret.", e);
        }
    }

    /**
     * Put secret key in keystore under given alias. Secret key is secured with the entry password.
     * @param alias alias to secret key
     * @param key secret key that will be stored in keystore
     * @param entryPassword password to secret key
     * @throws KeyStoreProviderException
     */
    public void putSecretKey(String alias, SecretKey key, String entryPassword) throws KeyStoreProviderException {
        try {
            LOG.info(String.format("Putting secret key with alias %s in keystore ...", alias));

            this.keystore.setEntry(alias, new KeyStore.SecretKeyEntry(key),
                    new KeyStore.PasswordProtection(entryPassword.toCharArray()));
            factory.persist(this.keystore);
        } catch (KeyStoreException kse) {
            throw new KeyStoreProviderException("Failed to put secret key to keystore.", kse);
        } catch (Exception e) {
            throw new KeyStoreProviderException("Failed to put secret key in keystore", e);
        }
    }

    /**
     * Gets the secret key stored in keystore under given alias.
     * @param alias
     * @param entryPassword entry password to access the secret key stored in keystore
     * @return the secret key or null if secret key does not exists in keystore
     * @throws KeyStoreProviderException
     */
    public SecretKey getSecretKey(String alias, String entryPassword) throws KeyStoreProviderException {
        try {
            LOG.info(String.format("Getting secret key with alias %s from keystore ...", alias));

            Optional<KeyStore.SecretKeyEntry> entry = Optional.fromNullable((KeyStore.SecretKeyEntry)this.keystore.getEntry(alias, new KeyStore.PasswordProtection(entryPassword.toCharArray())));

            if (!entry.isPresent()) {
                return null;
            }

            return entry.get().getSecretKey();

        } catch (NoSuchAlgorithmException nsae) {
            throw new KeyStoreProviderException("Algorithm for recovering the secret key cannot be found.", nsae);
        } catch (UnrecoverableEntryException uee) {
            throw new KeyStoreProviderException("Invalid entry password to recover secret.", uee);
        } catch (KeyStoreException kse) {
            throw new KeyStoreProviderException("Failed to get secret key entry.", kse);
        } catch (Exception e) {
            throw new KeyStoreProviderException("Failed to get secret key.", e);
        }
    }
}
