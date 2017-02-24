package org.osc.core.broker.service.response;

import java.io.File;

public class DownloadAgentLogResponse implements Response {

    private File supportBundle;

    public File getSupportBundle() {
        return supportBundle;
    }

    public void setSupportBundle(File supportBundle) {
        this.supportBundle = supportBundle;
    }

}
