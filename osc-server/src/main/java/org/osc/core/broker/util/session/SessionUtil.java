package org.osc.core.broker.util.session;

import javax.ws.rs.core.HttpHeaders;

public interface SessionUtil {
    void setUser(String user);

    String getCurrentUser();

    String getUsername(HttpHeaders headers);
}
