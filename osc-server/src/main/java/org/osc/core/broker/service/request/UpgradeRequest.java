package org.osc.core.broker.service.request;

import java.io.File;



public class UpgradeRequest implements Request {
    File uploadedFile; // File will point to fully qualified path and name, e.g:
                       // /tmp/serverUpgradeBundle.zip

    public File getUploadedFile() {
        return this.uploadedFile;
    }

    public void setUploadedFile(File uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

}
