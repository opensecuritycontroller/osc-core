package org.osc.core.broker.rest.client.openstack.vmidc.api;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.jclouds.openstack.keystone.v2_0.domain.Token;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudAuthentication;
import org.osc.core.broker.rest.client.openstack.vmidc.OSNovaClient;

public class OSNovaRestApi {
    Logger log = Logger.getLogger(OSNovaRestApi.class);

    private Token token;
    protected OSNovaClient osNovaClient;

    public OSNovaRestApi(Endpoint endPoint) throws IOException {

        JCloudAuthentication authApi = new JCloudAuthentication(endPoint);

        this.token = authApi.getTenantAccess().getToken();
        this.osNovaClient = new OSNovaClient(endPoint, this.token.getId());
        authApi.close();
    }

}
