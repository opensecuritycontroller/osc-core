package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.persistence.DistributedApplianceEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.util.encryption.EncryptionException;


public class ListDistributedApplianceService extends
        ServiceDispatcher<BaseRequest<BaseDto>, ListResponse<DistributedApplianceDto>> {

    ListResponse<DistributedApplianceDto> response = new ListResponse<DistributedApplianceDto>();

    @Override
    public ListResponse<DistributedApplianceDto> exec(BaseRequest<BaseDto> request, Session session) throws EncryptionException {
        // Initializing Entity Manager
        EntityManager<DistributedAppliance> emgr = new EntityManager<DistributedAppliance>(DistributedAppliance.class,
                session);
        // to do mapping
        List<DistributedApplianceDto> dtoList = new ArrayList<DistributedApplianceDto>();

        // mapping all the da objects to da dto objects
        for (DistributedAppliance da : emgr.listAll(new Order[] { Order.asc("name") })) {

            DistributedApplianceDto dto = new DistributedApplianceDto();

            DistributedApplianceEntityMgr.fromEntity(da, dto);
            if(request.isApi()) {
                DistributedApplianceDto.sanitizeDistributedAppliance(dto);
            }
            dtoList.add(dto);
        }

        this.response.setList(dtoList);
        return this.response;
    }

}
