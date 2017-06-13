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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.io.File;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.request.BackupRequest;
import org.osc.core.broker.service.response.BackupResponse;

@RunWith(MockitoJUnitRunner.class)
public class BackupServiceTest {

    @Mock
    private EncryptionApi encryption;

    @InjectMocks
	private BackupService target;

	private EntityManager em;

	@Before
	public void setUp() {
		this.em = mock(EntityManager.class);
		this.target = mock(BackupService.class);
	}

	@Test
	public void testExec_withValidFilenameAndPassword_PerformsFullFlow() throws Exception {
		// Arrange.
		byte[] backupZipBytes = "Test zip bytes".getBytes();
		BackupRequest request = new BackupRequest();
		BackupData expectedData = new BackupData();
		expectedData.setDbPassword("SOME_TEST_DB_PASSWORD");
		expectedData.setDbData("Test zip bytes".getBytes());
		expectedData.setAesCTRKeyHex("Some random hex");
		byte[] expectedSerialized = expectedData.serialize();

		String backupFilePath = "example/backup/file.dbb";
		request.setBackupFileName(backupFilePath);
		request.setBackupPassword("testPassword");

		when(this.target.exec(request, this.em)).thenCallRealMethod();
		when(this.target.getBackupZipFileBytes(backupFilePath)).thenReturn(backupZipBytes);
		when(this.target.getDBPassword()).thenReturn(expectedData.getDbPassword());
		when(this.target.getAESCTRKeyHex()).thenReturn(expectedData.getAesCTRKeyHex());

		// Act.
		BackupResponse response = this.target.exec(request, this.em);

		// Assert.
		verify(this.target, times(1)).ensureBackupFolderExists();
		verify(this.target, times(1)).deleteBackupFiles();
		verify(this.target, times(1)).createBackupZipFile(this.em, backupFilePath);
		verify(this.target, times(1)).getBackupZipFileBytes(backupFilePath);
		verify(this.target, times(1)).getDBPassword();
		verify(this.target, times(1)).getAESCTRKeyHex();

		verify(this.target, times(1)).encryptBackupFileBytes(expectedSerialized, request.getBackupPassword());
		assertTrue(response.isSuccess());
	}

	@Test
	public void testDeleteBackupFiles_withNoArguments_callsDeleteDefaultBackupFiles() {
		// Arrange.
		doCallRealMethod().when(this.target).deleteBackupFiles();

		// Act.
		this.target.deleteBackupFiles();

		// Assert.
		verify(this.target, times(1)).deleteBackupFiles("BrokerServerDBBackup");
	}

	@Test
	public void testDeleteBackupFiles_callsDeleteBothEncryptedAndZipFiles() {
		// Arrange.
		doCallRealMethod().when(this.target).deleteBackupFiles("BrokerServerDBBackup");
		doCallRealMethod().when(this.target).resolveBackupZipPath("BrokerServerDBBackup");
		doCallRealMethod().when(this.target).resolveEncryptedBackupPath("BrokerServerDBBackup");

		// Act.
		this.target.deleteBackupFiles("BrokerServerDBBackup");

		// Assert.
		verify(this.target, times(1)).deleteFile("backups" + File.separator + "BrokerServerDBBackup.zip");
		verify(this.target, times(1)).deleteFile("backups" + File.separator + "BrokerServerDBBackup.dbb");
	}
}
