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

import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.openstack.request.BaseOpenStackRequest;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.response.ListResponse;

public class ListRegionService extends ServiceDispatcher<BaseOpenStackRequest, ListResponse<String>> {

    private ListResponse<String> response = new ListResponse<String>();

    @Override
    public ListResponse<String> exec(BaseOpenStackRequest request, Session session) throws Exception {
        List<String> regions = new ArrayList<String>();
        EntityManager<VirtualSystem> emgr = new EntityManager<VirtualSystem>(VirtualSystem.class, session);
        // to do mapping
        VirtualizationConnector vc = emgr.findByPrimaryKey(request.getId()).getVirtualizationConnector();
        JCloudNova novaApi = new JCloudNova(new Endpoint(vc, request.getTenantName()));
        try {
            for (String region : novaApi.listRegions()) {
                regions.add(region);
            }
            this.response.setList(regions);
        } finally {
            if (novaApi != null) {
                novaApi.close();
            }
        }
        return this.response;
    }

}
