package org.osc.core.agent.rest.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;

import org.osc.core.util.AuthUtil;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

public class AgentAuthFilter implements ResourceFilter, ContainerRequestFilter {
    public static final String AGENT_DEFAULT_LOGIN = "agent";
    public static String AGENT_DEFAULT_PASS = "admin123";

    @Context
    HttpServletRequest request;
    @Context
    HttpServletResponse response;

    @Override
    public ContainerRequest filter(ContainerRequest req) {

        AuthUtil.authenticate(request, AGENT_DEFAULT_LOGIN, AGENT_DEFAULT_PASS);

        return req;
    }

    @Override
    public ContainerRequestFilter getRequestFilter() {
        return this;
    }

    @Override
    public ContainerResponseFilter getResponseFilter() {
        return null;
    }

}
