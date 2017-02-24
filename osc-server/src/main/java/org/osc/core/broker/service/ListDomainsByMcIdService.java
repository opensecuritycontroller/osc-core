package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.service.dto.DomainDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListDomainsByMcIdService extends ServiceDispatcher<BaseIdRequest, ListResponse<DomainDto>> {

    @Override
    public ListResponse<DomainDto> exec(BaseIdRequest request, Session session) throws Exception {

        ListResponse<DomainDto> response = new ListResponse<DomainDto>();

        Long mcId = request.getId();

        ApplianceManagerConnector mc = (ApplianceManagerConnector) session.get(ApplianceManagerConnector.class, mcId);

        if (mc == null) {
            throw new VmidcBrokerValidationException(
                    "Appliance Manager Connector with ID " + request.getId() + " is not found.");
        }

        EntityManager<Domain> em = new EntityManager<Domain>(Domain.class, session);
        List<Domain> entityList = em.findByParentId("applianceManagerConnector", mcId,
                new Order[] { Order.asc("name") });

        List<DomainDto> dtoList = new ArrayList<DomainDto>();
        for (Domain domain : entityList) {

            DomainDto dto = new DomainDto();
            dto.setId(domain.getId());
            dto.setName(domain.getName());

            dtoList.add(dto);
        }

        response.setList(dtoList);

        return response;

    }

}
