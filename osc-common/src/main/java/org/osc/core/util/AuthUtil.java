package org.osc.core.util;

import com.google.common.collect.ImmutableMap;
import com.sun.jersey.core.util.Base64;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Map;

public class AuthUtil {

    private static final Logger log = Logger.getLogger(AuthUtil.class);

    public static void authenticate(HttpServletRequest request, String validUserName, String validPass) {
        authenticate(request, ImmutableMap.of(validUserName, validPass));
    }

    /**
     * Checks if the request contains the username password combination from the given map of username and password.
     *
     */
    public static void authenticate(HttpServletRequest request, Map<String, String> usernamePasswordMap) {

        String authHeader = request.getHeader("Authorization");
        WebApplicationException wae = new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                .header("WWW-Authenticate", "Basic").entity("not authorized").build());

        if (authHeader == null) {
            log.warn("Authentication of " + request.getRequestURI() + " failed as auth header was null");
            throw wae;
        } else {
            String[] tokens = authHeader.trim().split("\\s+");

            if (tokens.length != 2 || !tokens[0].equalsIgnoreCase("BASIC")) {
                log.warn("Authentication of " + request.getRequestURI() + " failed as auth header does not have the right tokens");
                throw wae;
            }

            // valid auth header format, now need to authenticate the right user
            byte[] decodedBytes = Base64.decode(tokens[1]);
            String credString = new String(decodedBytes);

            String[] credentials = credString.split(":");

            if (credentials.length != 2) {
                log.warn("Authentication of " + request.getRequestURI() + " failed because of invalid credentials");
                throw wae;
            }

            String loginName = credentials[0];
            String password = credentials[1];

            if (!(containsString(loginName, usernamePasswordMap.keySet(), true)
                    && containsString(password, usernamePasswordMap.values(), false))) {
                log.warn("Authentication of " + request.getRequestURI() + " failed because of invalid credentials");
                throw wae;
            }
        }
    }

    private static boolean containsString(String stringToSearch, Collection<String> strings, boolean ignoreCase) {
        if (!ignoreCase) {
            return strings.contains(stringToSearch);
        } else {
            for (String string : strings) {
                if (string.equalsIgnoreCase(stringToSearch)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static void authenticateLocalRequest(HttpServletRequest request) {
        WebApplicationException wae = new WebApplicationException(
                Response.status(Response.Status.UNAUTHORIZED).entity("not authorized").build());

        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || !remoteAddr.equals("127.0.0.1")) {
            throw wae;
        }

    }
}
