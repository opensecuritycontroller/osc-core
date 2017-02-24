package org.osc.core.broker.service.alert;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.osc.core.broker.model.entities.events.Alert;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.persistence.AlertEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListAlertService extends ServiceDispatcher<BaseRequest<BaseDto>, ListResponse<AlertDto>> {

    @Override
    public ListResponse<AlertDto> exec(BaseRequest<BaseDto> request, Session session) throws Exception {

        // Initializing Entity Manager
        EntityManager<Alert> emgr = new EntityManager<Alert>(Alert.class, session);

        List<AlertDto> alertList = new ArrayList<AlertDto>();

        for (Alert alert : emgr.listAll(new Order[] { Order.desc("createdTimestamp") })) {
            AlertDto dto = new AlertDto();
            AlertEntityMgr.fromEntity(alert, dto);
            alertList.add(dto);
        }
        ListResponse<AlertDto> response = new ListResponse<AlertDto>();
        response.setList(alertList);
        return response;
    }
}
