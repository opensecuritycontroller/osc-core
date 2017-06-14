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

import org.osc.core.broker.model.entities.events.Alarm;
import org.osc.core.broker.service.dto.AlarmDto;

public class AlarmEntityMgr {

    public static Alarm createEntity(AlarmDto dto) {
        Alarm alarm = new Alarm();
        toEntity(alarm, dto);
        return alarm;
    }

    public static void toEntity(Alarm alarm, AlarmDto dto) {
        // transform from dto to entity
        alarm.setId(dto.getId());
        alarm.setEnable(dto.isEnabledAlarm());
        alarm.setName(dto.getName());
        alarm.setEventType(dto.getEventType());
        alarm.setRegexMatch(dto.getRegexMatch());
        alarm.setSeverity(dto.getSeverity());
        alarm.setAlarmAction(dto.getAlarmAction());
        alarm.setReceipientEmail(dto.getReceipientEmail());
    }

    public static void fromEntity(Alarm alarm, AlarmDto dto) {
        dto.setId(alarm.getId());
        dto.setEnabledAlarm(alarm.isEnabled());
        dto.setName(alarm.getName());
        dto.setEventType(alarm.getEventType());
        dto.setRegexMatch(alarm.getRegexMatch());
        dto.setSeverity(alarm.getSeverity());
        dto.setAlarmAction(alarm.getAlarmAction());
        dto.setReceipientEmail(alarm.getReceipientEmail());
    }
}
