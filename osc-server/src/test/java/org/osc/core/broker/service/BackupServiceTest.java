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
		BackupRequest request = new BackupRequest();
		byte[] backupZipBytes = "Test zip bytes".getBytes();
		byte[] backupZipWithPasswordBytes = "Test zip bytes with password".getBytes();
		
		String backupFilePath = "example/backup/file.dbb";
		request.setBackupFileName(backupFilePath);
		request.setBackupPassword("testPassword");
		
		when(target.exec(request, sessionMock)).thenCallRealMethod();
		when(target.getBackupZipFileBytes(backupFilePath)).thenReturn(backupZipBytes);
		when(target.appendDBPassword(backupZipBytes)).thenReturn(backupZipWithPasswordBytes);

		// Act.
		BackupResponse response = target.exec(request, sessionMock);
		
		// Assert.
		verify(target, times(1)).ensureBackupFolderExists();
		verify(target, times(1)).deleteBackupFiles();
		verify(target, times(1)).createBackupZipFile(sessionMock, backupFilePath);
		verify(target, times(1)).appendDBPassword(backupZipBytes);
		verify(target, times(1)).encryptBackupFileBytes(backupZipWithPasswordBytes, request.getBackupPassword());
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
	
	@Test
	public void testAppendPasswordBytes_withGivenBytes_appendsPasswordProperly() throws Exception {
		// Arrange.
		byte[] testBytes = "someTestBytes".getBytes();
		byte[] testDBPasswordBytes = "dbPasswordBytes".getBytes();
		
		when(target.appendDBPassword(testBytes)).thenCallRealMethod();
		when(target.getDBPasswordBytes()).thenReturn(testDBPasswordBytes);
		
		// Act.
		byte[] testBytesWithDBPassword = target.appendDBPassword(testBytes);
		
		// Assert.
		assertEquals(testBytesWithDBPassword.length, 160 /* max password length (fixed) */ + testBytes.length);
	}
}
