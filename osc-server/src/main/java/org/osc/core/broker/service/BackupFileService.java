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
package org.osc.core.broker.service;

import java.io.IOException;
import java.util.Properties;

import javax.crypto.SecretKey;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.Response;
import org.osc.core.server.Server;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.FileUtil;
import org.osc.core.util.KeyStoreProvider;
import org.osc.core.util.KeyStoreProvider.KeyStoreProviderException;

abstract class BackupFileService<I extends Request, O extends Response> extends ServiceDispatcher<I, O> {
	protected static final String DEFAULT_BACKUP_FILE_NAME = "BrokerServerDBBackup";
	
	protected static final String BACKUPS_FOLDER = "backups";
	protected static final String EXT_ENCRYPTED_BACKUP = ".dbb";
    protected static final String EXT_ZIP_BACKUP = ".zip";
    protected static final String DATABASE_FILENAME = "vmiDCDB.h2.db";
    
    protected static final int DB_PASSWORD_MAX_LENGTH = 160;
    
    protected static final Logger log = Logger.getLogger(BackupService.class);
    
    public boolean isValidBackupFilename(String filename) {
    	return isValidZipBackupFilename(filename) ||
    			isValidEncryptedBackupFilename(filename);
    }
    
    public boolean isValidZipBackupFilename(String filename) {
    	return ("." + FilenameUtils.getExtension(filename)).equals(EXT_ZIP_BACKUP);
    }
    
    public boolean isValidEncryptedBackupFilename(String filename) {
    	return ("." + FilenameUtils.getExtension(filename)).equals(EXT_ENCRYPTED_BACKUP);
    }
    
    protected Properties getProperties() throws IOException {
    	return FileUtil.loadProperties(Server.CONFIG_PROPERTIES_FILE);
    }
    
    protected byte[] encryptBackupFileBytes(byte[] backupFileBytes, String password) throws Exception {
    	EncryptionParameters params = new EncryptionParameters();
        return EncryptionUtil.encryptAESGCM(backupFileBytes, params.getKey(), params.getIV(), password.getBytes("UTF-8"));
    }
    
    protected byte[] decryptBackupFileBytes(byte[] backupFileBytes, String password) throws Exception {
    	EncryptionParameters params = new EncryptionParameters();
        return EncryptionUtil.decryptAESGCM(backupFileBytes, params.getKey(), params.getIV(), password.getBytes("UTF-8"));
    }
    
    private class EncryptionParameters {
        EncryptionParameters() throws KeyStoreProviderException, IOException {
    		// get aliases/passwords to keystore from properties
    		Properties properties = getProperties();
            String keyAlias = properties.getProperty("db.backup.key.alias");
            String keyPassword = properties.getProperty("db.backup.key.password");
            String ivAlias = properties.getProperty("db.backup.iv.alias");
            String ivPassword = properties.getProperty("db.backup.iv.password");
            
            // get key and iv from keystore
            key = KeyStoreProvider.getInstance().getSecretKey(keyAlias, keyPassword);
            iv = DatatypeConverter.parseHexBinary(KeyStoreProvider.getInstance().getPassword(ivAlias, ivPassword));
    	}
        
        // get key and iv from keystore
        private SecretKey key;
        private byte[] iv;
        
        public SecretKey getKey() {
        	return key;
        }
        
        public byte[] getIV() {
        	return iv;
        }
    }
}
