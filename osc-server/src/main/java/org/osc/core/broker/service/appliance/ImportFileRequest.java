package org.osc.core.broker.service.appliance;

import org.osc.core.broker.service.request.Request;

public class ImportFileRequest implements Request {

    private String uploadPath;

    public ImportFileRequest(String uploadPath) {
        this.uploadPath = uploadPath;
    }

    public String getUploadPath() {
        return this.uploadPath;
    }

}
