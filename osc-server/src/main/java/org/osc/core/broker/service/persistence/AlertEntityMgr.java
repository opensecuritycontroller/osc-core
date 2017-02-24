package org.osc.core.broker.service.persistence;

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.events.Alert;
import org.osc.core.broker.service.alert.AlertDto;

public class AlertEntityMgr {

    public static Alert createEntity(AlertDto dto) {
        Alert alert = new Alert();
        toEntity(alert, dto);
        return alert;
    }

    public static void toEntity(Alert alert, AlertDto dto) {

        // transform from dto to entity
        alert.setId(dto.getId());
        alert.setName(dto.getName());
        alert.setType(dto.getEventType());
        if (dto.getObject() != null) {
            alert.setObjectId(dto.getObject().getId());
            alert.setObjectType(dto.getObject().getType());
            alert.setObjectName(dto.getObject().getName());
        }
        alert.setSeverity(dto.getSeverity());
        alert.setStatus(dto.getStatus());
        alert.setMessage(dto.getMessage());
        alert.setAcknowledgedBy(dto.getAcknowledgedUser());
        alert.setTimeAcknowledgedTimestamp(dto.getTimeAcknowledgedTimestamp());
        alert.setCreatedTimestamp(dto.getTimeCreatedTimestamp());
    }

    public static void fromEntity(Alert alert, AlertDto dto) {

        dto.setId(alert.getId());
        dto.setName(alert.getName());
        dto.setEventType(alert.getType());

        if (alert.getObjectId() != null) {
            LockObjectReference object = new LockObjectReference(alert.getObjectId(), alert.getObjectName(),
                    alert.getObjectType());
            dto.setObject(object);
        }
        dto.setSeverity(alert.getSeverity());
        dto.setStatus(alert.getStatus());
        dto.setMessage(alert.getMessage());
        dto.setAcknowledgedUser(alert.getAcknowledgedBy());
        dto.setTimeAcknowledgedTimestamp(alert.getTimeAcknowledgedTimestamp());
        dto.setTimeCreatedTimestamp(alert.getCreatedTimestamp());
    }
}
