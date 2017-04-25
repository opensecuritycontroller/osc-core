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
package org.osc.core.broker.service.openstack;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.jclouds.openstack.keystone.v2_0.domain.Tenant;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudKeyStone;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.ListTenantByVcIdServiceApi;
import org.osc.core.broker.service.dto.openstack.TenantBean;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListTenantByVcIdService extends ServiceDispatcher<BaseIdRequest, ListResponse<TenantBean>>
        implements ListTenantByVcIdServiceApi {

    private ListResponse<TenantBean> response = new ListResponse<>();

    @Override
    public ListResponse<TenantBean> exec(BaseIdRequest request, EntityManager em) throws Exception {

        // Initializing Entity Manager
        OSCEntityManager<VirtualizationConnector> emgr = new OSCEntityManager<VirtualizationConnector>(VirtualizationConnector.class, em);

        // to do mapping
        VirtualizationConnector vc = emgr.findByPrimaryKey(request.getId());
        JCloudKeyStone keystoneApi = new JCloudKeyStone(new Endpoint(vc));
        try {
            List<TenantBean> tenantList = new ArrayList<>();

            for (Tenant tenant : keystoneApi.listTenants()) {
                tenantList.add(new TenantBean(tenant.getName()));
            }

            this.response.setList(tenantList);
        } finally {
            if (keystoneApi != null) {
                keystoneApi.close();
            }
        }

        return this.response;

    }
}
