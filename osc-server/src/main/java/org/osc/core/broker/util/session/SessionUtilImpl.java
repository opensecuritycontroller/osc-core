package org.osc.core.broker.util.session;

import com.vaadin.ui.UI;

import javax.ws.rs.core.HttpHeaders;
import java.util.Base64;
import java.util.StringTokenizer;

public class SessionUtilImpl implements SessionUtil {
    private final ThreadLocal<String> local = new ThreadLocal<String>();

    public void setUser(String user) {
        local.set(user);
    }

    private String getUser() {
        return local.get();
    }

    public String getCurrentUser() {
        if (UI.getCurrent() != null && UI.getCurrent().getSession() != null
                ) {
            return (String) UI.getCurrent().getSession().getAttribute("user");
        } else {
            return getUser();
        }
    }

    public String getUsername(HttpHeaders headers) {
        String authString = headers.getRequestHeader("Authorization").get(0);
        // Get encoded username and password
        final String encodedUserPassword = authString.replaceFirst("Basic ", "");

        // Decode username and password
        String usernameAndPassword = new String(Base64.getDecoder().decode(encodedUserPassword));

        // Split username and password tokens
        final StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, ":");
        final String username = tokenizer.nextToken();
        return username;
    }
}
