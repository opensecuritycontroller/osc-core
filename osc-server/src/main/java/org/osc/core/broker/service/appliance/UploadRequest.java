package org.osc.core.broker.service.appliance;

import java.io.InputStream;

import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.request.BaseRequest;


public class UploadRequest extends BaseRequest<BaseDto> {

    private String fileName;
    private InputStream uploadedInputStream;

    public UploadRequest(String fileName, InputStream uploadedInputStream) {
        this.fileName = fileName;
        this.uploadedInputStream = uploadedInputStream;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public InputStream getUploadedInputStream() {
        return this.uploadedInputStream;
    }

    public void setUploadedInputStream(InputStream uploadedInputStream) {
        this.uploadedInputStream = uploadedInputStream;
    }
}
