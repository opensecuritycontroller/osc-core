package org.osc.core.broker.service.request;

import java.util.HashSet;
import java.util.Set;

public class UpdateDaiConsolePasswordRequest implements Request {

    private String vsName;
    private Set<String> daiList = new HashSet<String>();
    private String newPassword;

    public String getVsName() {
        return vsName;
    }

    public void setVsName(String vsName) {
        this.vsName = vsName;
    }

    public Set<String> getDaiList() {
        return daiList;
    }

    public void setDaiList(Set<String> daiList) {
        this.daiList = daiList;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @Override
    public String toString() {
        return "UpdateConsolePasswordRequest [vsName=" + getVsName() + ", daiList=" + getDaiList() + "]";
    }
}
