package org.osc.core.broker.service.openstack;

import org.hibernate.Session;
import org.jclouds.openstack.keystone.v2_0.domain.Tenant;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudKeyStone;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListTenantByVcIdService extends ServiceDispatcher<BaseIdRequest, ListResponse<Tenant>> {

    private ListResponse<Tenant> response = new ListResponse<Tenant>();

    @Override
    public ListResponse<Tenant> exec(BaseIdRequest request, Session session) throws Exception {

        // Initializing Entity Manager
        EntityManager<VirtualizationConnector> emgr = new EntityManager<VirtualizationConnector>(VirtualizationConnector.class, session);

        // to do mapping
        VirtualizationConnector vc = emgr.findByPrimaryKey(request.getId());
        JCloudKeyStone keystoneApi = new JCloudKeyStone(new Endpoint(vc));
        try {

            this.response.setList(keystoneApi.listTenants());
        } finally {
            if (keystoneApi != null) {
                keystoneApi.close();
            }
        }

        return this.response;

    }
}
