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

import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;
import org.osc.core.broker.service.request.BackupRequest;
import org.osc.core.broker.service.response.BackupResponse;

import static org.mockito.Mockito.*;

import java.io.File;

import static org.junit.Assert.*;

public class BackupServiceTest {
	
	private BackupService target;
	private Session sessionMock;
	
	@Before
	public void setUp() {
		sessionMock = mock(Session.class);
		target = mock(BackupService.class);
	}

	@Test
	public void testExec_withValidFilenameAndPassword_PerformsFullFlow() throws Exception {
		// Arrange.
		byte[] backupZipBytes = "Test zip bytes".getBytes();
		BackupRequest request = new BackupRequest();
		BackupData expectedData = new BackupData();
		expectedData.dbPassword = "SOME_TEST_DB_PASSWORD";
		expectedData.dbData = "Test zip bytes".getBytes();
		expectedData.aesCTRKeyHex = "Some random hex";
		byte[] expectedSerialized = expectedData.serialize();

		String backupFilePath = "example/backup/file.dbb";
		request.setBackupFileName(backupFilePath);
		request.setBackupPassword("testPassword");
		
		when(target.exec(request, sessionMock)).thenCallRealMethod();
		when(target.getBackupZipFileBytes(backupFilePath)).thenReturn(backupZipBytes);
		when(target.getDBPassword()).thenReturn(expectedData.dbPassword);
		when(target.getAESCTRKeyHex()).thenReturn(expectedData.aesCTRKeyHex);

		// Act.
		BackupResponse response = target.exec(request, sessionMock);
		
		// Assert.
		verify(target, times(1)).ensureBackupFolderExists();
		verify(target, times(1)).deleteBackupFiles();
		verify(target, times(1)).createBackupZipFile(sessionMock, backupFilePath);
		verify(target, times(1)).getBackupZipFileBytes(backupFilePath);
		verify(target, times(1)).getDBPassword();
		verify(target, times(1)).getAESCTRKeyHex();

		verify(target, times(1)).encryptBackupFileBytes(expectedSerialized, request.getBackupPassword());
		assertTrue(response.isSuccess());
	}
	
	@Test
	public void testDeleteBackupFiles_withNoArguments_callsDeleteDefaultBackupFiles() {
		// Arrange.
		doCallRealMethod().when(target).deleteBackupFiles();
		
		// Act.
		target.deleteBackupFiles();
		
		// Assert.
		verify(target, times(1)).deleteBackupFiles("BrokerServerDBBackup");
	}
	
	@Test
	public void testDeleteBackupFiles_callsDeleteBothEncryptedAndZipFiles() {
		// Arrange.
		doCallRealMethod().when(target).deleteBackupFiles("BrokerServerDBBackup");
		doCallRealMethod().when(target).resolveBackupZipPath("BrokerServerDBBackup");
		doCallRealMethod().when(target).resolveEncryptedBackupPath("BrokerServerDBBackup");
		
		// Act.
		target.deleteBackupFiles("BrokerServerDBBackup");
		
		// Assert.
		verify(target, times(1)).deleteFile("backups" + File.separator + "BrokerServerDBBackup.zip");
		verify(target, times(1)).deleteFile("backups" + File.separator + "BrokerServerDBBackup.dbb");
	}
}
