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

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.openstack4j.model.identity.v3.Region;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4jKeystone;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.ListRegionByVcIdServiceApi;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseOpenStackRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osgi.service.component.annotations.Component;

@Component
public class ListRegionByVcIdService extends ServiceDispatcher<BaseOpenStackRequest, ListResponse<String>>
        implements ListRegionByVcIdServiceApi {

    @Override
    public ListResponse<String> exec(BaseOpenStackRequest request, EntityManager em) throws Exception {
        OSCEntityManager<VirtualizationConnector> emgr = new OSCEntityManager<>(VirtualizationConnector.class, em, this.txBroadcastUtil);
        VirtualizationConnector vc = emgr.findByPrimaryKey(request.getId());

        ListResponse<String> listResponse = new ListResponse<>();
        try (Openstack4jKeystone keystone = new Openstack4jKeystone(new Endpoint(vc, request.getProjectName()))) {
            List<? extends Region> endpoints = keystone.getOs().identity().regions().list();
            List<String> regions = endpoints.stream().map(Region::getId).collect(Collectors.toList());
            listResponse.setList(regions);
        }
        return listResponse;
    }
}
