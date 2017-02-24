package org.osc.core.broker.service.alert;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.events.Alert;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.response.EmptySuccessResponse;

public class DeleteAlertService extends ServiceDispatcher<AlertRequest, EmptySuccessResponse> {

    @Override
    public EmptySuccessResponse exec(AlertRequest request, Session session) throws Exception {
        EmptySuccessResponse response = new EmptySuccessResponse();

        for (AlertDto dto : request.getDtoList()) {
            Alert alert = (Alert) session.get(Alert.class, dto.getId());
            if (alert == null) {
                throw new VmidcBrokerValidationException("Alarm entry with id " + dto.getId() + " is not found.");
            }
            EntityManager.delete(session, alert);
        }
        return response;
    }
}
