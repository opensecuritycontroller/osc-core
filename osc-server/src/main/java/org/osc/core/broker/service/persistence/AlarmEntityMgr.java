package org.osc.core.broker.service.persistence;

import org.osc.core.broker.model.entities.events.Alarm;
import org.osc.core.broker.service.alarm.AlarmDto;

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
