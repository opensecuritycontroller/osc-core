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

import org.jclouds.openstack.neutron.v2.domain.Network;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNeutron;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.ListNetworkServiceApi;
import org.osc.core.broker.service.dto.openstack.NetworkBean;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseOpenStackRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListNetworkService extends ServiceDispatcher<BaseOpenStackRequest, ListResponse<NetworkBean>>
        implements ListNetworkServiceApi {

    private ListResponse<NetworkBean> response = new ListResponse<>();

    @Override
    public ListResponse<NetworkBean> exec(BaseOpenStackRequest request, EntityManager em) throws Exception {

        // Initializing Entity Manager
        OSCEntityManager<VirtualSystem> emgr = new OSCEntityManager<VirtualSystem>(VirtualSystem.class, em);

        // to do mapping
        VirtualizationConnector vc = emgr.findByPrimaryKey(request.getId()).getVirtualizationConnector();

        JCloudNeutron neutronApi = new JCloudNeutron(new Endpoint(vc, request.getTenantName()));

        try {
            List<NetworkBean> networkList = new ArrayList<>();

            for (Network network : neutronApi.listNetworkByTenant(request.getRegion(), request.getTenantId())) {
                networkList.add(new NetworkBean(network.getName()));
            }

            this.response.setList(networkList);

        } finally {
            if (neutronApi != null) {
                neutronApi.close();
            }
        }
        return this.response;
    }
}
