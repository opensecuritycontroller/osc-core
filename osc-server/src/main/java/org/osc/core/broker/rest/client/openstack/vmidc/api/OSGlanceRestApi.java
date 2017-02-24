package org.osc.core.broker.rest.client.openstack.vmidc.api;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.jclouds.openstack.keystone.v2_0.domain.Token;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudAuthentication;
import org.osc.core.broker.rest.client.openstack.vmidc.OSGlanceClient;

public class OSGlanceRestApi {
    Logger log = Logger.getLogger(OSGlanceRestApi.class);

    private Token token;
    protected OSGlanceClient osGlanceClient;

    public OSGlanceRestApi(Endpoint endPoint) throws IOException {

        JCloudAuthentication authApi = new JCloudAuthentication(endPoint);

        this.token = authApi.getTenantAccess().getToken();
        this.osGlanceClient = new OSGlanceClient(endPoint, this.token.getId());
        authApi.close();
    }

}
