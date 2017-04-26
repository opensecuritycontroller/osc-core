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
package org.osc.core.broker.service.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.model.entities.events.AlarmAction;
import org.osc.core.broker.model.entities.events.EventType;
import org.osc.core.broker.model.entities.events.Severity;

import io.swagger.annotations.ApiModelProperty;

@XmlRootElement(name = "alarm")
@XmlAccessorType(XmlAccessType.FIELD)
public class AlarmDto extends BaseDto {

    @ApiModelProperty(required = true)
    private boolean enabledAlarm;

    @ApiModelProperty(required = true)
    private String name;

    @ApiModelProperty(required = true)
    private EventType eventType;

    @ApiModelProperty(required = true)
    private String regexMatch;

    @ApiModelProperty(required = true)
    private Severity severity;

    @ApiModelProperty(required = true)
    private AlarmAction alarmAction;

    @ApiModelProperty(value = "If Alarm action is email, then this field is required.")
    private String receipientEmail;

    public boolean isEnabledAlarm() {
        return this.enabledAlarm;
    }

    public void setEnabledAlarm(boolean enabled) {
        this.enabledAlarm = enabled;
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
        return "AlarmDto [enabled=" + this.enabledAlarm + ", name=" + this.name + ", eventType=" + this.eventType
                + ", regexMatch=" + this.regexMatch + ", severity=" + this.severity + ", alarmAction="
                + this.alarmAction + ", receipientEmail=" + this.receipientEmail + ", getId()=" + getId() + "]";
    }

}
