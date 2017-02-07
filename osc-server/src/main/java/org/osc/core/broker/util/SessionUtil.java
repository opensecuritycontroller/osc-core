package org.osc.core.broker.util;

import java.util.Base64;
import java.util.List;
import java.util.StringTokenizer;

import javax.ws.rs.core.HttpHeaders;

import com.vaadin.ui.UI;

public class SessionUtil {
    private static final ThreadLocal<String> local = new ThreadLocal<String>();

    public static void setUser(String user) {
        local.set(user);
    }

    private static String getUser() {
        return local.get();
    }

    public static String getCurrentUser() {
        if (UI.getCurrent() != null && UI.getCurrent().getSession() != null
                ) {
            return (String) UI.getCurrent().getSession().getAttribute("user");
        } else {
            return getUser();
        }
    }

    public static String getUsername(HttpHeaders headers) {
        List<String> authorizationHeader = headers.getRequestHeader("Authorization");

        if(authorizationHeader == null || authorizationHeader.size() == 0){
            throw new IllegalArgumentException("Basic authorization is not set");
        }

        String authString = authorizationHeader.get(0);
        // Get encoded username and password
        final String encodedUserPassword = authString.replaceFirst("Basic ", "");

        // Decode username and password
        String usernameAndPassword = new String(Base64.getDecoder().decode(encodedUserPassword));

        // Split username and password tokens
        final StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, ":");
        return tokenizer.nextToken();
    }

}
