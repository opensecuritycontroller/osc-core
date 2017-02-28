/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.rest.client.openstack.jcloud;

import java.io.Closeable;
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

    public JCloudKeyStone(Endpoint endPoint) {
        super(endPoint);
        this.keystoneApi = JCloudUtil.buildApi(KeystoneApi.class, OPENSTACK_SERVICE_KEYSTONE, endPoint);
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

    @Override
    protected List<? extends Closeable> getApis() {
        return Arrays.asList(this.keystoneApi);
    }
}
