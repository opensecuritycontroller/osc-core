package org.osc.core.util;

import org.glassfish.jersey.server.ContainerRequest;
import org.osc.core.rest.annotations.LocalHostAuth;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Auth filter which makes sure the request is coming from the same machine where the request is being processed
 * (localhost or 127.0.0.1)
 */
@Provider
@LocalHostAuth
public class LocalHostAuthFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        AuthUtil.authenticateLocalRequest((ContainerRequest) containerRequestContext);
    }
}
