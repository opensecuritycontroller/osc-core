package org.osc.core.broker.rest.client.openstack.vmidc;

import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.rest.client.RestBaseClient;

public class OSNovaClient extends RestBaseClient {
    private static final String OPENSTACK_REST_URL_BASE = "/v2";
    public static final int NOVA_PORT = 8774;

    Logger log = Logger.getLogger(OSNovaClient.class);

    public OSNovaClient(Endpoint endPoint, String token) {

        super(OPENSTACK_REST_URL_BASE, MediaType.APPLICATION_JSON);

        initRestBaseClient(endPoint.getEndPointIP(), NOVA_PORT, endPoint.getUser(), endPoint.getPassword(),
                endPoint.isHttps());

        this.headerKey = "X-Auth-Token";
        this.headerKeyValue = token;
    }

}
