package org.osc.core.broker.util;

import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.view.util.EventType;

public class BroadcastMessage {

    private Long entityId;
    private String receiver;
    private EventType eventType;
    private BaseDto dto;

    public BroadcastMessage(Long entityId, String receiver, EventType eventType) {
        this(entityId, receiver, eventType, null);
    }

    public BroadcastMessage(Long entityId, String receiver, EventType eventType, BaseDto dto) {
        this.entityId = entityId;
        this.receiver = receiver;
        this.eventType = eventType;
        this.dto = dto;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    @Override
    public String toString() {
        return "BroadcastMessage [entityId=" + entityId + ", receiver=" + receiver + ", eventType=" + eventType + "]";
    }

    public BaseDto getDto() {
        return this.dto;
    }
}
