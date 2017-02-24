package org.osc.core.broker.rest.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;

import org.osc.core.util.AuthUtil;

import com.google.common.collect.ImmutableMap;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

public class AgentAuthFilter implements ResourceFilter, ContainerRequestFilter {
    public static final String VMIDC_AGENT_LOGIN = "agent";
    public static String VMIDC_AGENT_PASS = "";

    @Context
    HttpServletRequest request;
    @Context
    HttpServletResponse response;

    @Override
    public ContainerRequest filter(ContainerRequest req) {

        AuthUtil.authenticate(this.request, ImmutableMap.of(VMIDC_AGENT_LOGIN, VMIDC_AGENT_PASS,
                VmidcAuthFilter.VMIDC_DEFAULT_LOGIN, VmidcAuthFilter.VMIDC_DEFAULT_PASS));

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
