package org.osc.core.broker.service.request;

public class BackupRequest implements Request {

    private String backupFileName;
    private String backupPassword;
    
    public String getBackupFileName() {
        return backupFileName;
    }

    public void setBackupFileName(String backupFileName) {
        this.backupFileName = backupFileName;
    }
    
    public String getBackupPassword() {
    	return this.backupPassword;
    }
    
    public void setBackupPassword(String backupPassword) {
    	this.backupPassword = backupPassword;
    }
}
