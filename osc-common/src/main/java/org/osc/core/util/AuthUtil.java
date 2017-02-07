package org.osc.core.util;

import com.google.common.collect.ImmutableMap;
import org.apache.log4j.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;

public class AuthUtil {

    private static final Logger log = Logger.getLogger(AuthUtil.class);

    public static void authenticate(ContainerRequestContext request, String validUserName, String validPass) {
        authenticate(request, ImmutableMap.of(validUserName, validPass));
    }

    /**
     * Checks if the request contains the username password combination from the given map of username and password.
     *
     */
    public static void authenticate(ContainerRequestContext request, Map<String, String> usernamePasswordMap) {

        String authHeader = request.getHeaderString("Authorization");
        WebApplicationException wae = new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                .header("WWW-Authenticate", "Basic").entity("not authorized").build());

        String requestUri = request.getUriInfo()!=null?request.getUriInfo().getBaseUri().toString():"";
        if (authHeader == null) {
            log.warn("Authentication of " + requestUri + " failed as auth header was null");
            throw wae;
        } else {
            String[] tokens = authHeader.trim().split("\\s+");

            if (tokens.length != 2 || !tokens[0].equalsIgnoreCase("BASIC")) {
                log.warn("Authentication of " + requestUri + " failed as auth header does not have the right tokens");
                throw wae;
            }

            // valid auth header format, now need to authenticate the right user
            byte[] decodedBytes = Base64.getDecoder().decode(tokens[1]);
            String credString = new String(decodedBytes);

            String[] credentials = credString.split(":");

            if (credentials.length != 2) {
                log.warn("Authentication of " + requestUri + " failed because of invalid credentials");
                throw wae;
            }

            String loginName = credentials[0];
            String password = credentials[1];

            if (!(containsString(loginName, usernamePasswordMap.keySet(), true)
                    && containsString(password, usernamePasswordMap.values(), false))) {
                log.warn("Authentication of " + requestUri + " failed because of invalid credentials");
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

    public static void authenticateLocalRequest(ContainerRequestContext request) {
        WebApplicationException wae = new WebApplicationException(
                Response.status(Response.Status.UNAUTHORIZED).entity("not authorized").build());
        if(request.getUriInfo()!=null && request.getUriInfo().getRequestUri()!=null) {
            String remoteAddr = request.getUriInfo().getRequestUri().getHost();
            if (remoteAddr == null || !remoteAddr.equals("127.0.0.1")) {
                throw wae;
            }
        } else {
            throw wae;
        }

    }
}
