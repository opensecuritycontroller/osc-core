package org.osc.core.broker.service.openstack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.jclouds.openstack.nova.v2_0.domain.regionscoped.AvailabilityZone;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.dto.openstack.AvailabilityZoneDto;
import org.osc.core.broker.service.openstack.request.BaseOpenStackRequest;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.response.ListResponse;

public class ListAvailabilityZonesService extends
        ServiceDispatcher<BaseOpenStackRequest, ListResponse<AvailabilityZoneDto>> {

    private ListResponse<AvailabilityZoneDto> response = new ListResponse<AvailabilityZoneDto>();

    @Override
    public ListResponse<AvailabilityZoneDto> exec(BaseOpenStackRequest request, Session session) throws IOException {
        List<AvailabilityZoneDto> azList = new ArrayList<AvailabilityZoneDto>();
        // Initializing Entity Manager
        EntityManager<VirtualSystem> emgr = new EntityManager<VirtualSystem>(VirtualSystem.class, session);

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
            this.response.setList(azList);

        } finally {
            if (novaApi != null) {
                novaApi.close();
            }

        }
        return this.response;
    }
}
