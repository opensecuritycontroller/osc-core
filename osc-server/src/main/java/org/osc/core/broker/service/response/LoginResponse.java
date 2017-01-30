package org.osc.core.broker.service.response;

public class LoginResponse implements Response {

    private long userID;

    public long getUserID() {
        return userID;
    }

    public void setUserID(long userID) {
        this.userID = userID;
    }

}
