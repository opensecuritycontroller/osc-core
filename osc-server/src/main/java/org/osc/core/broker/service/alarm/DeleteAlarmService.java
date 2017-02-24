package org.osc.core.broker.service.alarm;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.events.Alarm;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;

public class DeleteAlarmService extends ServiceDispatcher<BaseIdRequest, EmptySuccessResponse> {

    @Override
    public EmptySuccessResponse exec(BaseIdRequest request, Session session) throws Exception {

        Alarm alarm = (Alarm) session.get(Alarm.class, request.getId());

        if (alarm == null) {
            throw new VmidcBrokerValidationException("Alarm entry with id " + request.getId() + " is not found.");
        }

        EntityManager.delete(session, alarm);
        return new EmptySuccessResponse();
    }
}
