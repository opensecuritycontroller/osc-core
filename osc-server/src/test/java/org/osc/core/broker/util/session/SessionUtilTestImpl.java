package org.osc.core.broker.util.session;

import javax.ws.rs.core.HttpHeaders;

public class SessionUtilTestImpl implements SessionUtil {
    @Override
    public void setUser(String user) {
        System.out.println("setUser called");
    }

    @Override
    public String getCurrentUser() {
        System.out.println("getCurrentUser called");
        return null;
    }

    @Override
    public String getUsername(HttpHeaders headers) {
        System.out.println("getUsername called");
        return null;
    }
}
