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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.osc.core.broker.service.request.BackupRequest;
import org.osc.core.broker.service.response.BackupResponse;

public class BackupServiceTest {

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
		BackupRequest request = new BackupRequest();
		byte[] backupZipBytes = "Test zip bytes".getBytes();
		byte[] backupZipWithPasswordBytes = "Test zip bytes with password".getBytes();
		byte[] backupZipWithPasswordAndAESCTRBytes = "Test zip bytes with password and AES-CTR key".getBytes();

		String backupFilePath = "example/backup/file.dbb";
		request.setBackupFileName(backupFilePath);
		request.setBackupPassword("testPassword");

		when(this.target.exec(request, this.em)).thenCallRealMethod();
		when(this.target.getBackupZipFileBytes(backupFilePath)).thenReturn(backupZipBytes);
		when(this.target.appendDBPassword(backupZipBytes)).thenReturn(backupZipWithPasswordBytes);
		when(this.target.appendAESCTRKey(backupZipWithPasswordBytes)).thenReturn(backupZipWithPasswordAndAESCTRBytes);

		// Act.
		BackupResponse response = this.target.exec(request, this.em);

		// Assert.
		verify(this.target, times(1)).ensureBackupFolderExists();
		verify(this.target, times(1)).deleteBackupFiles();
		verify(this.target, times(1)).createBackupZipFile(this.em, backupFilePath);
		verify(this.target, times(1)).appendDBPassword(backupZipBytes);
		verify(this.target, times(1)).encryptBackupFileBytes(backupZipWithPasswordAndAESCTRBytes, request.getBackupPassword());
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

	@Test
	public void testAppendPasswordBytes_withGivenBytes_appendsPasswordProperly() throws Exception {
		// Arrange.
		byte[] testBytes = "someTestBytes".getBytes();
		byte[] testDBPasswordBytes = "dbPasswordBytes".getBytes();

		when(this.target.appendDBPassword(testBytes)).thenCallRealMethod();
		when(this.target.getDBPasswordBytes()).thenReturn(testDBPasswordBytes);

		// Act.
		byte[] testBytesWithDBPassword = this.target.appendDBPassword(testBytes);

		// Assert.
		assertEquals(testBytesWithDBPassword.length, 160 /* max password length (fixed) */ + testBytes.length);
	}
}
