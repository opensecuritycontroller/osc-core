package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListDeploymentSpecServiceByVirtualSystem extends
        ServiceDispatcher<BaseIdRequest, ListResponse<DeploymentSpecDto>> {

    private VirtualSystem vs;
    ListResponse<DeploymentSpecDto> response = new ListResponse<DeploymentSpecDto>();

    @Override
    public ListResponse<DeploymentSpecDto> exec(BaseIdRequest request, Session session) throws Exception {

        validateAndLoad(request, session);
        // to do mapping
        List<DeploymentSpecDto> dtoList = new ArrayList<DeploymentSpecDto>();

        // mapping all the da objects to da dto objects
        for (DeploymentSpec ds : DeploymentSpecEntityMgr
                .listDeploymentSpecByVirtualSystem(session, this.vs, new Order[] { Order.asc("name") })) {
            DeploymentSpecDto dto = new DeploymentSpecDto();
            DeploymentSpecEntityMgr.fromEntity(ds, dto);
            dtoList.add(dto);
        }
        this.response.setList(dtoList);
        return this.response;
    }

    private void validateAndLoad(BaseIdRequest req, Session session) throws Exception {
        BaseIdRequest.checkForNullId(req);
        EntityManager<VirtualSystem> emgr = new EntityManager<VirtualSystem>(VirtualSystem.class, session);
        this.vs = emgr.findByPrimaryKey(req.getId());
        if (this.vs == null) {
            throw new VmidcBrokerValidationException("Virtual System with Id: " + req.getId() + "  is not found.");
        }

    }
}
