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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.ObjectType;
import org.osc.core.common.alarm.EventType;
import org.osc.core.common.alarm.Severity;
import org.osc.core.common.job.AcknowledgementStatus;

@SuppressWarnings("serial")
@Entity
@Table(name = "ALERT")
public class Alert extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "object_name")
    private String objectName;

    @Column(name = "object_id")
    private Long objectId;

    @Column(name = "object_type")
    @Enumerated(EnumType.STRING)
    private ObjectType objectType;

    @Column(name = "severity", nullable = false)
    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType type;

    @Column(name = "message")
    private String message;

    @Column(name = "acknowledgement_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AcknowledgementStatus status;

    @Column(name = "time_acknowledged_timestamp")
    private Date timeAcknowledgedTimestamp;

    @Column(name = "acknowledged_by")
    private String acknowledgedBy;

    public Alert() {
        super();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getObjectName() {
        return this.objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public Long getObjectId() {
        return this.objectId;
    }

    public void setObjectId(Long objectId) {
        this.objectId = objectId;
    }

    public ObjectType getObjectType() {
        return this.objectType;
    }

    public void setObjectType(ObjectType objectType) {
        this.objectType = objectType;
    }

    public Severity getSeverity() {
        return this.severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public EventType getType() {
        return this.type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public AcknowledgementStatus getStatus() {
        return this.status;
    }

    public void setStatus(AcknowledgementStatus status) {
        this.status = status;
    }

    public Date getTimeAcknowledgedTimestamp() {
        return this.timeAcknowledgedTimestamp;
    }

    public void setTimeAcknowledgedTimestamp(Date timeAcknowledgedTimestamp) {
        this.timeAcknowledgedTimestamp = timeAcknowledgedTimestamp;
    }

    public String getAcknowledgedBy() {
        return this.acknowledgedBy;
    }

    public void setAcknowledgedBy(String acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Alert [name=" + this.name + ", objectName=" + this.objectName + ", objectId=" + this.objectId + ", objectType="
                + this.objectType + ", severity=" + this.severity + ", type=" + this.type + ", message=" + this.message + ", status="
                + this.status + ", timeAcknowledgedTimestamp=" + this.timeAcknowledgedTimestamp + ", acknowledgedBy="
                + this.acknowledgedBy + "]";
    }
}