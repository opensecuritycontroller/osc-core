package org.osc.core.broker.service.openstack;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIPPool;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.openstack.request.BaseOpenStackRequest;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.response.ListResponse;

public class ListFloatingIpPoolsService extends ServiceDispatcher<BaseOpenStackRequest, ListResponse<String>> {

    @Override
    public ListResponse<String> exec(BaseOpenStackRequest request, Session session) throws Exception {
        ListResponse<String> response = new ListResponse<>();

        EntityManager<VirtualSystem> emgr = new EntityManager<>(VirtualSystem.class, session);

        VirtualizationConnector vc = emgr.findByPrimaryKey(request.getId()).getVirtualizationConnector();

        JCloudNova novaApi = new JCloudNova(new Endpoint(vc, request.getTenantName()));

        try {
            List<? extends FloatingIPPool> osFloatingIpPoolsList = novaApi.getFloatingIpPools(request.getRegion());
            List<String> floatingIpPools = new ArrayList<>();

            for (FloatingIPPool floatingIpPool : osFloatingIpPoolsList) {
                floatingIpPools.add(floatingIpPool.getName());
            }

            response.setList(floatingIpPools);
        } finally {
            if(novaApi != null) {
                novaApi.close();
            }
        }

        return response;
    }

}
