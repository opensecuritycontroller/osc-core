package org.osc.core.broker.service.response;

public class LoginResponse implements Response {

    private long userID;

    private boolean passwordChangeNeeded;

    public long getUserID() {
        return userID;
    }

    public void setUserID(long userID) {
        this.userID = userID;
    }

    public boolean isPasswordChangeNeeded() {
        return passwordChangeNeeded;
    }

    public void setPasswordChangeNeeded(boolean passwordChangeNeeded) {
        this.passwordChangeNeeded = passwordChangeNeeded;
    }
}
