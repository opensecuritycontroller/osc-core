package org.osc.core.agent.rest.server;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import org.osc.core.rest.annotations.AgentAuth;
import org.osc.core.util.AuthUtil;

import java.io.IOException;

@Provider
@AgentAuth
public class AgentAuthFilter implements ContainerRequestFilter {
    public static final String AGENT_DEFAULT_LOGIN = "agent";
    public static String AGENT_DEFAULT_PASS = "admin123";

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        AuthUtil.authenticate(containerRequestContext, AGENT_DEFAULT_LOGIN, AGENT_DEFAULT_PASS);
    }
}
