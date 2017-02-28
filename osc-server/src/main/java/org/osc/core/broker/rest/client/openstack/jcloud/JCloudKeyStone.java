package org.osc.core.broker.rest.client.openstack.jcloud;

import java.io.Closeable;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.jclouds.openstack.keystone.v2_0.KeystoneApi;
import org.jclouds.openstack.keystone.v2_0.domain.Tenant;
import org.jclouds.openstack.keystone.v2_0.features.TenantApi;

import com.google.common.collect.FluentIterable;

public class JCloudKeyStone extends BaseJCloudApi {

    static final String OPENSTACK_SERVICE_KEYSTONE = "openstack-keystone";
    private final KeystoneApi keystoneApi;
    private final String endpointUrl;

    public JCloudKeyStone(Endpoint endPoint) throws MalformedURLException, URISyntaxException {
        super(endPoint);
        this.keystoneApi = JCloudUtil.buildApi(KeystoneApi.class, OPENSTACK_SERVICE_KEYSTONE, endPoint);
        this.endpointUrl = JCloudUtil.prepareEndpointURL(endPoint);
    }

    public List<Tenant> listTenants() {
        FluentIterable<Tenant> listIter = getTenantApi().list().concat();

        //Sort List by name
        List<Tenant> tenants = listIter.toSortedList(new Comparator<Tenant>() {

            @Override
            public int compare(Tenant o1, Tenant o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        return tenants;
    }

    public Tenant getTenantById(String tenantId) {
        return getTenantApi().get(tenantId);
    }

    public Tenant getTenant(String tenantName) {
        return getTenantApi().getByName(tenantName);
    }

    private TenantApi getTenantApi() {
        return getOptionalOrThrow(this.keystoneApi.getTenantApi(), "Tenant Api");
    }

    public String getEndpointUrl() {
        return this.endpointUrl;
    }

    @Override
    protected List<? extends Closeable> getApis() {
        return Arrays.asList(this.keystoneApi);
    }
}
