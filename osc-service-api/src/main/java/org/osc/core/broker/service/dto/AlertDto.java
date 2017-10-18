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

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.service.dto.job.LockObjectDto;
import org.osc.core.common.alarm.EventType;
import org.osc.core.common.alarm.Severity;
import org.osc.core.common.job.AcknowledgementStatus;

import io.swagger.annotations.ApiModelProperty;

@XmlRootElement(name = "alert")
@XmlAccessorType(XmlAccessType.FIELD)
public class AlertDto extends BaseDto {
    @ApiModelProperty(required=true)
    private String name;

    private LockObjectDto object;

    @ApiModelProperty(required=true)
    private Severity severity;

    @ApiModelProperty(required=true)
    private EventType eventType;

    @ApiModelProperty(required=true)
    @XmlElement(name = "status")
    private AcknowledgementStatus status;

    private String message;
    private String acknowledgedUser;
    private Date timeAcknowledgedTimestamp;
    private Date timeCreatedTimestamp;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LockObjectDto getObject() {
        return this.object;
    }

    public void setObject(LockObjectDto object) {
        this.object = object;
    }

    public Severity getSeverity() {
        return this.severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public EventType getEventType() {
        return this.eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public AcknowledgementStatus getStatus() {
        return this.status;
    }

    public void setStatus(AcknowledgementStatus status) {
        this.status = status;
    }

    public String getAcknowledgedUser() {
        return this.acknowledgedUser;
    }

    public void setAcknowledgedUser(String acknowledgedUser) {
        this.acknowledgedUser = acknowledgedUser;
    }

    public Date getTimeAcknowledgedTimestamp() {
        return this.timeAcknowledgedTimestamp;
    }

    public void setTimeAcknowledgedTimestamp(Date timeAcknowledgedTimestamp) {
        this.timeAcknowledgedTimestamp = timeAcknowledgedTimestamp;
    }

    public Date getTimeCreatedTimestamp() {
        return this.timeCreatedTimestamp;
    }

    public void setTimeCreatedTimestamp(Date timeCreatedTimestamp) {
        this.timeCreatedTimestamp = timeCreatedTimestamp;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "AlertDto [name=" + this.name + ", object=" + this.object + ", severity=" + this.severity + ", eventType=" + this.eventType
                + ", status=" + this.status + ", acknowledgedUser=" + this.acknowledgedUser + ", timeAcknowledgedTimestamp="
                + this.timeAcknowledgedTimestamp + ", timeCreatedTimestamp=" + this.timeCreatedTimestamp + ", getId()=" + getId()
                + "]";
    }

}
