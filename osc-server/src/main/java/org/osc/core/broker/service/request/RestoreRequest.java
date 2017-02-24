package org.osc.core.broker.service.request;

import java.io.File;

public class RestoreRequest implements Request {

    File bkpFile;
    String password;
    
    public File getBkpFile() {
        return bkpFile;
    }

    public void setBkpFile(File bkpFile) {
        this.bkpFile = bkpFile;
    }
    
    public String getPassword() {
    	return this.password;
    }
    
    public void setPassword(String password) {
    	this.password = password;
    }

    @Override
    public String toString() {
        return "RestoreRequest [bkpFile=" + bkpFile + "]";
    }
    

}
