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

import org.openstack4j.model.compute.HostAggregate;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNova;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.ListHostAggregateServiceApi;
import org.osc.core.broker.service.dto.openstack.HostAggregateDto;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseOpenStackRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osgi.service.component.annotations.Component;

@Component
public class ListHostAggregateService extends ServiceDispatcher<BaseOpenStackRequest, ListResponse<HostAggregateDto>>
        implements ListHostAggregateServiceApi {

    @Override
    public ListResponse<HostAggregateDto> exec(BaseOpenStackRequest request, EntityManager em) throws Exception {

        OSCEntityManager<VirtualSystem> emgr = new OSCEntityManager<>(VirtualSystem.class, em, this.txBroadcastUtil);
        VirtualizationConnector vc = emgr.findByPrimaryKey(request.getId()).getVirtualizationConnector();

        Openstack4JNova novaApi = new Openstack4JNova(new Endpoint(vc, request.getTenantName()));
        List<? extends HostAggregate> hostAggregatesList = novaApi.listHostAggregates(request.getRegion());
        List<HostAggregateDto> hostAggrDtoList = hostAggregatesList.stream().map(this::toHostAggregateDto).collect(Collectors.toList());
        return new ListResponse<>(hostAggrDtoList);
    }

    private HostAggregateDto toHostAggregateDto(HostAggregate ha) {
        HostAggregateDto dto = new HostAggregateDto();
        dto.setName(ha.getName());
        dto.setOpenstackId(ha.getId());
        return dto;
    }
}
