package org.osc.core.broker.rest.server;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import org.osc.core.rest.annotations.OscAuth;
import org.osc.core.util.AuthUtil;

import java.io.IOException;

@Provider
@OscAuth
public class OscAuthFilter implements ContainerRequestFilter {

    public static final String OSC_DEFAULT_LOGIN = "admin";
    public static String OSC_DEFAULT_PASS = "";

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        AuthUtil.authenticate(containerRequestContext, OSC_DEFAULT_LOGIN, OSC_DEFAULT_PASS);
    }

}
