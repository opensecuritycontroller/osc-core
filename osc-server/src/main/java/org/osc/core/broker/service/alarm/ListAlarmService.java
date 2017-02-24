package org.osc.core.broker.service.alarm;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.osc.core.broker.model.entities.events.Alarm;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.persistence.AlarmEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListAlarmService extends ServiceDispatcher<BaseRequest<BaseDto>, ListResponse<AlarmDto>> {

    @Override
    public ListResponse<AlarmDto> exec(BaseRequest<BaseDto> request, Session session) throws Exception {
        // Initializing Entity Manager
        EntityManager<Alarm> emgr = new EntityManager<Alarm>(Alarm.class, session);

        List<AlarmDto> alarmList = new ArrayList<AlarmDto>();

        // Mapping all the Alarm objects to Alarm dto objects
        for (Alarm alarm : emgr.listAll(new Order[] { Order.asc("name") })) {
            AlarmDto dto = new AlarmDto();
            AlarmEntityMgr.fromEntity(alarm, dto);
            alarmList.add(dto);
        }
        ListResponse<AlarmDto> response = new ListResponse<AlarmDto>();
        response.setList(alarmList);
        return response;
    }
}
