package org.osc.core.broker.service.openstack;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.dto.openstack.HostDto;
import org.osc.core.broker.service.openstack.request.BaseOpenStackRequest;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.response.ListResponse;

public class ListHostService extends ServiceDispatcher<BaseOpenStackRequest, ListResponse<HostDto>> {

    @Override
    public ListResponse<HostDto> exec(BaseOpenStackRequest request, Session session) throws Exception {
        ListResponse<HostDto> response = new ListResponse<>();

        EntityManager<VirtualSystem> emgr = new EntityManager<>(VirtualSystem.class, session);
        VirtualizationConnector vc = emgr.findByPrimaryKey(request.getId()).getVirtualizationConnector();

        JCloudNova novaApi = null;
        try {
            novaApi = new JCloudNova(new Endpoint(vc, request.getTenantName()));

            List<HostDto> hostList = new ArrayList<>();
            for (String host : novaApi.getComputeHosts(request.getRegion())) {
                hostList.add(new HostDto(host, host));
            }

            response.setList(hostList);

        } finally {
            if (novaApi != null) {
                novaApi.close();
            }
        }

        return response;

    }

}
