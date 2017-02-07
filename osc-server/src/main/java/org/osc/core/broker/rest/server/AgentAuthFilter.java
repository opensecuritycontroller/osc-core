package org.osc.core.broker.rest.server;

import com.google.common.collect.ImmutableMap;
import org.osc.core.rest.annotations.AgentAuth;
import org.osc.core.util.AuthUtil;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import java.io.IOException;

@Provider
@AgentAuth
public class AgentAuthFilter implements ContainerRequestFilter {

    public static final String VMIDC_AGENT_LOGIN = "agent";
    public static String VMIDC_AGENT_PASS = "admin123";

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        AuthUtil.authenticate(containerRequestContext, ImmutableMap.of(VMIDC_AGENT_LOGIN, VMIDC_AGENT_PASS,
                OscAuthFilter.OSC_DEFAULT_LOGIN, OscAuthFilter.OSC_DEFAULT_PASS));
    }

}
