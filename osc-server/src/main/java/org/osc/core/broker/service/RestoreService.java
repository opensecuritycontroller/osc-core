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

import com.mcafee.vmidc.server.Server;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.hibernate.Session;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.request.RestoreRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;
import org.osc.core.broker.util.db.DBConnectionParameters;
import org.osc.core.broker.util.db.RestoreUtil;
import org.osc.core.rest.client.crypto.X509TrustManagerFactory;
import org.osc.core.util.KeyStoreProvider;
import org.osc.core.util.ServerUtil;
import org.osc.core.util.encryption.EncryptionException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RestoreService extends BackupFileService<RestoreRequest, EmptySuccessResponse> {

    @Override
    public EmptySuccessResponse exec(RestoreRequest request, Session session) throws Exception {
    	File backupFile = request.getBkpFile();
    	String backupFilename = backupFile.getName();
    	Server.setInMaintenance(true);
    	DBConnectionParameters connectionParams = new DBConnectionParameters();
    	String oldDBPassword = connectionParams.getPassword();
    	
    	// decrypt if needed
    	if (isValidEncryptedBackupFilename(backupFilename)) {
    		// hold reference to encrypted version to be able to delete it
    		File encryptedBackupFile = backupFile;
    		// replace the backup file with the decrypted version
    		try {
    			// decrypt backup blob
    			byte [] decryptedBLOB = decryptBackupFileBytes(FileUtils.readFileToByteArray(encryptedBackupFile), request.getPassword());
    			// parse backup blob
    			BackupData backupData = new BackupData();
    			backupData.deserialize(decryptedBLOB);
    			// write backup zip file with the same name
    			backupFile = backupData.writeBackupZipFile(FilenameUtils.removeExtension(backupFilename) + EXT_ZIP_BACKUP);
    			// update DB password (in keystore) with the one from backup blob
    			updateKeystore(connectionParams, backupData);
                // reload truststore from backup
                updateTruststore(backupData);
    		} catch(Exception e) {
    			Server.setInMaintenance(false);
    			throw e;
    		} finally {
    			encryptedBackupFile.delete();
    		}
    	} else if(isValidZipBackupFilename(backupFilename)){
    		// to support backward compatibility it is necessary to load the DB with the default password and
    		// generate new - secure one
    		connectionParams.restoreDefaultPassword();
    	}
    	
    	// restore h2 database to temporary file
    	RestoreUtil.restoreDataBase(backupFile, new File("tmp" + File.separator + ".").getAbsolutePath());
    	backupFile.delete();
    	
    	File newDBFileTemp = new File("tmp" + File.separator + DATABASE_FILENAME);
        try {
        	// check if one can access db and get db version
            RestoreUtil.validateRestoreBundle(newDBFileTemp);
        } catch (Exception ex) {
        	// restore old DB password
        	connectionParams.updatePassword(oldDBPassword);
            Server.setInMaintenance(false);
            throw ex;
        }

        boolean successRename = false;
        File originalDBFile = new File(DATABASE_FILENAME + ".bkp");
        try {
            originalDBFile.delete();
            // Temporary rename existing file to make room for new file
            log.info("Restore (pid:" + ServerUtil.getCurrentPid() + "): Renaming existing database File.");
            successRename = new File(DATABASE_FILENAME).renameTo(originalDBFile);
            if (!successRename) {
                // File was not successfully renamed
                throw new VmidcException("Fail to backup existing database file before restoring.");
            }
            log.info("Restore (pid:" + ServerUtil.getCurrentPid() + "): Restoring DataBase file.");
            FileUtils.copyFile(newDBFileTemp, new File(DATABASE_FILENAME));

            log.info("Restore: Starting restored Database server.");
            boolean successStarted = startNewServer();
            if (!successStarted) {
                throw new Exception("Fail to verify newly restored server is running.");
            }
            log.info("Restore (pid:" + ServerUtil.getCurrentPid() + "): Deleting original file.");
            // cleaning up tmp folder
            originalDBFile.delete();

        } catch (Exception ex) {
        	// restore old DB password
        	connectionParams.updatePassword(oldDBPassword);
            Server.setInMaintenance(false);
            if (successRename) {
            	newDBFileTemp.delete();
                originalDBFile.renameTo(newDBFileTemp);
            }
            log.error("Restore (pid:" + ServerUtil.getCurrentPid() + "): Error restoring Database.", ex);
            throw new VmidcException(ex.getMessage());
        } finally {
            request.getBkpFile().delete();
            File f = new File("tmp" + File.separator + DATABASE_FILENAME);
            if (f.exists()) {
                // deleting trace file generated during validation
                f.delete();
            }
        }
        return new EmptySuccessResponse();
    }

    private boolean startNewServer() {
        log.info("Restore Server (pid:" + ServerUtil.getCurrentPid() + "): Start new vmidc server.");
        return ServerUtil.startServerProcess();
    }

	private void updateKeystore(DBConnectionParameters connectionParams,BackupData backupData) throws EncryptionException, KeyStoreProvider.KeyStoreProviderException {
		connectionParams.updatePassword(backupData.dbPassword);
		backupData.updateAESCTRKeyInKeystore();
	}

    private void updateTruststore(BackupData backupData) throws IOException {
        // backup current truststore
        File oldTruststore = new File(X509TrustManagerFactory.TRUSTSTORE_FILE);
        if (oldTruststore.exists()) {
            oldTruststore.renameTo(new File(X509TrustManagerFactory.TRUSTSTORE_FILE + ".bak"));
        }

        try {
            // try to write new truststore
            Files.write(Paths.get(X509TrustManagerFactory.TRUSTSTORE_FILE), backupData.truststoreData);
        } catch (IOException e) {
            // delete new truststore if exists
            new File(X509TrustManagerFactory.TRUSTSTORE_FILE).delete();
            // move old truststore
            if (oldTruststore.exists()) {
                oldTruststore.renameTo(new File(X509TrustManagerFactory.TRUSTSTORE_FILE));
            }

            throw e;
        }

        // delete truststore backup (exists in case of success)
        new File(X509TrustManagerFactory.TRUSTSTORE_FILE + ".bak").delete();
    }
}
