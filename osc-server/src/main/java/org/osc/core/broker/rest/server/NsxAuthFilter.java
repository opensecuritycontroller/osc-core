package org.osc.core.broker.rest.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;

import org.osc.core.util.AuthUtil;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

public class NsxAuthFilter implements ResourceFilter, ContainerRequestFilter {
    public static final String VMIDC_NSX_LOGIN = "nsx";
    public static String VMIDC_NSX_PASS = "";

    @Context
    private HttpServletRequest request;
    @Context
    private HttpServletResponse response;

    @Override
    public ContainerRequest filter(ContainerRequest req) {

        AuthUtil.authenticate(request, VMIDC_NSX_LOGIN, VMIDC_NSX_PASS);

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
