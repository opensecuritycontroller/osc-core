package org.osc.core.util;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.osc.core.util.encryption.EncryptionException;

import com.google.common.collect.ImmutableMap;
import com.sun.jersey.core.util.Base64;

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
                log.warn("Authentication of " + request.getRequestURI() + " failed - invalid credentials format, auth header: " + authHeader + ", credstring " + credString);
                // TODO bartek: remove workaround below, NSX calls are failing without that workaround because the
                // credentials length is 3. For some reason tokens[1] was bnN4Ojc5NjYzMDY4NjE3NjA5RjRFQkRCQkI2RjRCQ0RFRUY3OkI1RDhDQjZBM0ZERUNDQkI=
                // and decoded nsx:79663068617609F4EBDBBB6F4BCDEEF7:B5D8CB6A3FDECCBB

                credentials[1] = "admin123";
                //throw wae;
            }

            String loginName = credentials[0];
            String password = credentials[1];


            if (!validateUserAndPassword(loginName, password, usernamePasswordMap)) {
                log.warn("Authentication of " + request.getRequestURI() + " failed - user password mismatch");
                throw wae;
            }
        }
    }

    private static boolean validateUserAndPassword(String loginName, String password, Map<String, String> usernamePasswordMap) {
        return usernamePasswordMap.entrySet().stream().anyMatch(entry -> {
            try {
                return entry.getKey().equalsIgnoreCase(loginName) && EncryptionUtil.validateAESCTR(password, entry.getValue());
            } catch (EncryptionException encryptionException) {
                log.error("Failed to validate AESCTR password", encryptionException);
                return false;
            }
        });
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
