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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.jclouds.openstack.nova.v2_0.domain.regionscoped.AvailabilityZone;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.ListAvailabilityZonesServiceApi;
import org.osc.core.broker.service.api.server.EncryptionException;
import org.osc.core.broker.service.dto.openstack.AvailabilityZoneDto;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseOpenStackRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osgi.service.component.annotations.Component;

@Component
public class ListAvailabilityZonesService
        extends ServiceDispatcher<BaseOpenStackRequest, ListResponse<AvailabilityZoneDto>>
        implements ListAvailabilityZonesServiceApi {

    @Override
    public ListResponse<AvailabilityZoneDto> exec(BaseOpenStackRequest request, EntityManager em) throws IOException, EncryptionException {
        ListResponse<AvailabilityZoneDto> response = new ListResponse<AvailabilityZoneDto>();
        List<AvailabilityZoneDto> azList = new ArrayList<AvailabilityZoneDto>();
        // Initializing Entity Manager
        OSCEntityManager<VirtualSystem> emgr = new OSCEntityManager<VirtualSystem>(VirtualSystem.class, em, this.txBroadcastUtil);

        // to do mapping
        VirtualizationConnector vc = emgr.findByPrimaryKey(request.getId()).getVirtualizationConnector();
        JCloudNova novaApi = new JCloudNova(new Endpoint(vc, request.getTenantName()));
        try {
            for (String region : novaApi.listRegions()) {
                for (AvailabilityZone az : novaApi.listAvailabilityZones(region)) {
                    AvailabilityZoneDto azDto = new AvailabilityZoneDto();
                    azDto.setRegion(region);
                    azDto.setZone(az.getName());

                    azList.add(azDto);
                }
            }
            response.setList(azList);

        } finally {
            if (novaApi != null) {
                novaApi.close();
            }

        }
        return response;
    }
}
