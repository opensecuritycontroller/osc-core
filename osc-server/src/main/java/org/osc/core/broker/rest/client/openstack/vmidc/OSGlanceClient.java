package org.osc.core.broker.rest.client.openstack.vmidc;

import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.rest.client.RestBaseClient;

public class OSGlanceClient extends RestBaseClient {
    private static final String OPENSTACK_REST_URL_BASE = "/v2";
    public static final int GLANCE_PORT = 9292;

    Logger log = Logger.getLogger(OSGlanceClient.class);

    public OSGlanceClient(Endpoint endPoint, String token) {

        super(OPENSTACK_REST_URL_BASE, MediaType.APPLICATION_JSON);

        initRestBaseClient(endPoint.getEndPointIP(), GLANCE_PORT, endPoint.getUser(), endPoint.getPassword(),
                endPoint.isHttps());

        this.headerKey = "X-Auth-Token";
        this.headerKeyValue = token;
    }

}
