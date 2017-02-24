package org.osc.core.broker.service.response;

public class BackupResponse implements Response {
    private boolean success;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

}
