package org.osc.core.broker.service.openstack;

import org.hibernate.Session;
import org.jclouds.openstack.neutron.v2.domain.Network;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNeutron;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.openstack.request.BaseOpenStackRequest;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.response.ListResponse;

public class ListNetworkService extends ServiceDispatcher<BaseOpenStackRequest, ListResponse<Network>> {

    private ListResponse<Network> response = new ListResponse<Network>();

    @Override
    public ListResponse<Network> exec(BaseOpenStackRequest request, Session session) throws Exception {

        // Initializing Entity Manager
        EntityManager<VirtualSystem> emgr = new EntityManager<VirtualSystem>(VirtualSystem.class, session);

        // to do mapping
        VirtualizationConnector vc = emgr.findByPrimaryKey(request.getId()).getVirtualizationConnector();

        JCloudNeutron neutronApi = new JCloudNeutron(new Endpoint(vc, request.getTenantName()));
        try {
            this.response.setList(neutronApi.listNetworkByTenant(request.getRegion(), request.getTenantId()));

        } finally {
            if (neutronApi != null) {
                neutronApi.close();
            }
        }
        return this.response;
    }
}
