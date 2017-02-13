package org.osc.core.broker.rest.server;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import org.osc.core.rest.annotations.NsxAuth;
import org.osc.core.util.AuthUtil;

import java.io.IOException;

@Provider
@NsxAuth
public class NsxAuthFilter implements ContainerRequestFilter {

    public static final String VMIDC_NSX_LOGIN = "nsx";
    public static String VMIDC_NSX_PASS = "";

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        AuthUtil.authenticate(containerRequestContext, VMIDC_NSX_LOGIN, VMIDC_NSX_PASS);
    }

}
