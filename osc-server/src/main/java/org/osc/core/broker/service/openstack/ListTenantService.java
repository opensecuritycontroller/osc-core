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

import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4jKeystone;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.ListTenantServiceApi;
import org.osc.core.broker.service.dto.openstack.OsTenantDto;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osgi.service.component.annotations.Component;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ListTenantService extends ServiceDispatcher<BaseIdRequest, ListResponse<OsTenantDto>>
        implements ListTenantServiceApi {

    @Override
    public ListResponse<OsTenantDto> exec(BaseIdRequest request, EntityManager em) throws Exception {

        // Initializing Entity Manager
        OSCEntityManager<VirtualSystem> emgr = new OSCEntityManager<>(VirtualSystem.class, em, this.txBroadcastUtil);

        // to do mapping
        VirtualizationConnector vc = emgr.findByPrimaryKey(request.getId()).getVirtualizationConnector();

        ListResponse<OsTenantDto> listResponse = new ListResponse<>();
        try (Openstack4jKeystone keystoneApi = new Openstack4jKeystone(new Endpoint(vc))) {
            List<OsTenantDto> tenantDtos = keystoneApi.listProjects().stream()
                    .map(tenant -> new OsTenantDto(tenant.getName(), tenant.getId())).collect(Collectors.toList());
            listResponse.setList(tenantDtos);
        }
        return listResponse;
    }
}
