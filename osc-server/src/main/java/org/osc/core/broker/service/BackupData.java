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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.osc.core.broker.service.api.server.EncryptionException;
import org.osc.core.broker.util.crypto.AESCTREncryption;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public final class BackupData implements Serializable {
    private static final long serialVersionUID = 4948762505615010097L;
    private String aesCTRKeyHex;
    private String dbPassword;
    private byte[] dbData;
    private byte[] truststoreData;

    public String getAesCTRKeyHex() {
        return aesCTRKeyHex;
    }

    public void setAesCTRKeyHex(String aesCTRKeyHex) {
        this.aesCTRKeyHex = aesCTRKeyHex;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public byte[] getDbData() {
        return dbData;
    }

    public void setDbData(byte[] dbData) {
        this.dbData = dbData;
    }

    public byte[] getTruststoreData() {
        return truststoreData;
    }

    public void setTruststoreData(byte[] truststoreData) {
        this.truststoreData = truststoreData;
    }

    public void deserialize(byte[] bytes) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

        try(ObjectInput in = new ObjectInputStream(bis)) {
            BackupData deserialized = (BackupData) in.readObject();
            aesCTRKeyHex = deserialized.aesCTRKeyHex;
            dbPassword = deserialized.dbPassword;
            dbData = deserialized.dbData;
            truststoreData = deserialized.truststoreData;
        }
    }

    public byte[] serialize() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try(ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(this);
            out.flush();
            return bos.toByteArray();
        }
    }

    public void updateAESCTRKeyInKeystore() throws EncryptionException {
        new AESCTREncryption().updateAESCTRKey(aesCTRKeyHex);
    }

    public File writeBackupZipFile(String filePath) throws IOException {
        File backupZipFile = new File(FilenameUtils.removeExtension(filePath) + BackupFileService.EXT_ZIP_BACKUP);
        FileUtils.writeByteArrayToFile(backupZipFile, dbData);
        return backupZipFile;
    }
}
