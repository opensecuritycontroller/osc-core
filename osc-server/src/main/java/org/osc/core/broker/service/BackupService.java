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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.osc.core.broker.service.request.BackupRequest;
import org.osc.core.broker.service.response.BackupResponse;
import org.osc.core.broker.util.db.DBConnectionParameters;
import org.osc.core.util.KeyStoreProvider.KeyStoreProviderException;
import org.osc.core.util.encryption.AESCTREncryption;
import org.osc.core.util.encryption.EncryptionException;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class BackupService extends BackupFileService<BackupRequest, BackupResponse> {


    @Override
    public BackupResponse exec(BackupRequest request, Session session) throws Exception {
        BackupResponse res = new BackupResponse();
        try {
        	// check for backup custom filename
            final String backupFileName = StringUtils.isBlank(request.getBackupFileName()) ? 
            							  DEFAULT_BACKUP_FILE_NAME : request.getBackupFileName();
            
            ensureBackupFolderExists();
            
            // delete old backup files
            deleteBackupFiles();
            
            // create temporary backup zip
            createBackupZipFile(session, backupFileName);
        	
            // get zip file bytes
            byte[] backupFileBytes = getBackupZipFileBytes(backupFileName);
            
            // concatenate zip file bytes with fixed length password bytes
            byte[] backupData = appendDBPassword(backupFileBytes);
            backupData = appendAESCTRKey(backupData);

            // encrypt the concatenation with AES-GCM
            byte[] encryptedBackupFileBytes = encryptBackupFileBytes(backupData, request.getBackupPassword());
            
            // remove temporary backup zip
            deleteFile(resolveBackupZipPath(backupFileName));
            
            // write encrypted backup file
            writeEncryptedBackupFile(backupFileName, encryptedBackupFileBytes);
            
            res.setSuccess(true);
        } catch (Exception ex) {
            res.setSuccess(false);
            log.error("Failed! to backup Database", ex);
        }
        return res;
    }
    
    void ensureBackupFolderExists() throws IOException {
    	FileUtils.forceMkdir(new File(BACKUPS_FOLDER));
    }
    
    public void deleteBackupFilesFrom(String directory) {
    	deleteBackupFilesFrom(directory, DEFAULT_BACKUP_FILE_NAME);
    }
    
    public void deleteBackupFilesFrom(String directory, String backupFileName) {
    	deleteFile(new StringBuilder().append(directory)
					    			  .append(File.separator)
					    			  .append(backupFileName)
					    			  .append(EXT_ENCRYPTED_BACKUP).toString());
    }
    
    public void deleteBackupFiles() {
    	deleteBackupFiles(DEFAULT_BACKUP_FILE_NAME);
    }
    
    public void deleteBackupFiles(String backupFileName) {
    	deleteFile(resolveBackupZipPath(backupFileName));
        deleteFile(resolveEncryptedBackupPath(backupFileName));
    }
    
    void deleteFile(String filePath) {
    	File file = new File(filePath);
        if (file.exists()) {
        	file.delete();
        }
    }

    void createBackupZipFile(Session session, String backupFileName) {
    	// create backup file
        session.doWork(new Work() {
            @Override
            @SuppressFBWarnings(value="SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
            public void execute(Connection connection) throws SQLException {
                log.info("Execute sql: " + "BACKUP TO '" + resolveBackupZipPath(backupFileName)  + "';");
                try (Statement statement = connection.createStatement()) {
                	statement.execute("BACKUP TO '" + resolveBackupZipPath(backupFileName) + "';");
                }
            }
        });
    }
    
    void writeEncryptedBackupFile(String backupFileName, byte[] encryptedBackupFileBytes) throws IOException {
    	FileUtils.writeByteArrayToFile(getEncryptedBackupFile(backupFileName), encryptedBackupFileBytes);
    }

    byte[] getBackupZipFileBytes(String backupFileName) throws IOException {
    	return FileUtils.readFileToByteArray(new File(resolveBackupZipPath(backupFileName)));
    }
    
    public File getEncryptedBackupFile() {
    	return getEncryptedBackupFile(DEFAULT_BACKUP_FILE_NAME);
    }
    
    public File getEncryptedBackupFile(String backupFileName) {
    	return new File(new StringBuilder().append(BACKUPS_FOLDER)
										   .append(File.separator)
										   .append(backupFileName)
										   .append(EXT_ENCRYPTED_BACKUP).toString());
    }

    String resolveBackupZipPath(String backupFileName) {
    	return new StringBuilder().append(BACKUPS_FOLDER)
    							  .append(File.separator)
    							  .append(backupFileName)
    							  .append(EXT_ZIP_BACKUP).toString();
    }

    String resolveEncryptedBackupPath(String backupFileName) {
    	return new StringBuilder().append(BACKUPS_FOLDER)
    							  .append(File.separator)
    							  .append(backupFileName)
    							  .append(EXT_ENCRYPTED_BACKUP).toString();
    }

    byte[] getDBPasswordBytes() throws UnsupportedEncodingException, KeyStoreProviderException, IOException {
    	return new DBConnectionParameters().getPassword().getBytes("UTF-8");
    }
    
    byte[] appendDBPassword(byte[] bytes) throws Exception {
    	ByteBuffer backupFileBytesBuffer = ByteBuffer.allocate(DB_PASSWORD_MAX_LENGTH + bytes.length);
        byte[] passwordBytes = getDBPasswordBytes();
        backupFileBytesBuffer.put(passwordBytes);
        backupFileBytesBuffer.position(DB_PASSWORD_MAX_LENGTH);
        backupFileBytesBuffer.put(bytes);
        return backupFileBytesBuffer.array();
    }

    byte[] appendAESCTRKey(byte[] bytes) throws EncryptionException {
        return new AESCTREncryption().appendAESCTRKey(bytes);
    }
}
