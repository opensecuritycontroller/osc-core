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
import org.osc.core.util.encryption.AESCTREncryption;
import org.osc.core.util.encryption.EncryptionException;

import java.io.*;

public final class BackupData implements Serializable {
    private static final long serialVersionUID = 4948762505615010097L;

    public String aesCTRKeyHex;
    public String dbPassword;
    public byte[] dbData;
    public byte[] truststoreData;

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
