package org.osc.core.broker.rest.client.openstack.jcloud;

import java.io.Closeable;
import java.util.Arrays;
import java.util.List;

import org.jclouds.openstack.keystone.v2_0.AuthenticationApi;
import org.jclouds.openstack.keystone.v2_0.domain.Access;
import org.jclouds.openstack.keystone.v2_0.domain.PasswordCredentials;

public class JCloudAuthentication extends BaseJCloudApi {

    private AuthenticationApi authentication;

    public JCloudAuthentication(Endpoint endPoint) {
        super(endPoint);
        this.authentication = JCloudUtil.buildApi(AuthenticationApi.class, JCloudKeyStone.OPENSTACK_SERVICE_KEYSTONE, endPoint);
    }

    public Access getTenantAccess() {
        return this.authentication.authenticateWithTenantNameAndCredentials(this.endPoint.getTenant(),
                PasswordCredentials.createWithUsernameAndPassword(this.endPoint.getUser(),this.endPoint.getPassword()));
    }

    @Override
    protected List<? extends Closeable> getApis() {
        return Arrays.asList(this.authentication);
    }
}
