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
package org.osc.core.broker.model.entities.events;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.common.alarm.AlarmAction;
import org.osc.core.common.alarm.EventType;
import org.osc.core.common.alarm.Severity;

@SuppressWarnings("serial")
@Entity
@Table(name = "ALARM")
public class Alarm extends BaseEntity {

    @Column(name = "enable_alarm", nullable = false)
    private Boolean enabled = true; // default value

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Column(name = "regex_match")
    private String regexMatch = ".*";

    @Column(name = "severity", nullable = false)
    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Column(name = "action", nullable = false)
    @Enumerated(EnumType.STRING)
    private AlarmAction alarmAction = AlarmAction.NONE; // default value

    @Column(name = "recipient_email")
    private String receipientEmail;

    public Alarm() {
        super();
    }

    public Boolean isEnabled() {
        return this.enabled;
    }

    public void setEnable(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EventType getEventType() {
        return this.eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getRegexMatch() {
        return this.regexMatch;
    }

    public void setRegexMatch(String regexMatch) {
        this.regexMatch = regexMatch;
    }

    public Severity getSeverity() {
        return this.severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public AlarmAction getAlarmAction() {
        return this.alarmAction;
    }

    public void setAlarmAction(AlarmAction alarmAction) {
        this.alarmAction = alarmAction;
    }

    public String getReceipientEmail() {
        return this.receipientEmail;
    }

    public void setReceipientEmail(String receipientEmail) {
        this.receipientEmail = receipientEmail;
    }

    @Override
    public String toString() {
        return "Alarm [enabled=" + this.enabled + ", name=" + this.name + ", eventType=" + this.eventType + ", regexMatch="
                + this.regexMatch + ", severity=" + this.severity + ", alarmAction=" + this.alarmAction + ", receipientEmail="
                + this.receipientEmail + ", getId()=" + getId() + "]";
    }
}
