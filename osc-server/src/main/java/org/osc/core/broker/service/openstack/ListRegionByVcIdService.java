package org.osc.core.broker.service.openstack;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.openstack.request.BaseOpenStackRequest;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.response.ListResponse;

public class ListRegionByVcIdService extends ServiceDispatcher<BaseOpenStackRequest, ListResponse<String>> {

    private ListResponse<String> response = new ListResponse<String>();

    @Override
    public ListResponse<String> exec(BaseOpenStackRequest request, Session session) throws Exception {
        List<String> regions = new ArrayList<String>();

        EntityManager<VirtualizationConnector> emgr = new EntityManager<VirtualizationConnector>(VirtualizationConnector.class, session);
        // to do mapping
        VirtualizationConnector vc = emgr.findByPrimaryKey(request.getId());
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
