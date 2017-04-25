/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.service.broadcast;

import org.osc.core.broker.service.dto.BaseDto;

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
        return this.entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getReceiver() {
        return this.receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public EventType getEventType() {
        return this.eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    @Override
    public String toString() {
        return "BroadcastMessage [entityId=" + this.entityId + ", receiver=" + this.receiver + ", eventType=" + this.eventType + "]";
    }

    public BaseDto getDto() {
        return this.dto;
    }
}
