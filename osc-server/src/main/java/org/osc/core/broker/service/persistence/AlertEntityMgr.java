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
package org.osc.core.broker.service.persistence;

import org.osc.core.broker.model.entities.ObjectType;
import org.osc.core.broker.model.entities.events.Alert;
import org.osc.core.broker.service.dto.AlertDto;
import org.osc.core.broker.service.dto.job.LockObjectDto;
import org.osc.core.broker.service.dto.job.ObjectTypeDto;

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
            alert.setObjectType(ObjectType.valueOf(dto.getObject().getType().getName()));
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
            LockObjectDto object = new LockObjectDto(alert.getObjectId(), alert.getObjectName(),
                    new ObjectTypeDto(alert.getObjectType().name(), alert.getObjectType().toString()));
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
