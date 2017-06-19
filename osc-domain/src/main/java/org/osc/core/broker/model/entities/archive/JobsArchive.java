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
package org.osc.core.broker.model.entities.archive;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.common.job.FreqType;
import org.osc.core.common.job.ThresholdType;

/**
 * JobsArchive Model
 */

@SuppressWarnings("serial")
@Entity
@Table(name = "JOBS_ARCHIVE")
public class JobsArchive extends BaseEntity {

    public JobsArchive() {
        super();
    }

    @Column(name = "frequency", nullable = false)
    @Enumerated(EnumType.STRING)
    private FreqType frequency = FreqType.WEEKLY; //default value

    @Column(name = "auto_schedule", nullable = false)
    private Boolean autoSchedule = false; //default value

    @Column(name = "threshold_unit", nullable = false)
    @Enumerated(EnumType.STRING)
    private ThresholdType thresholdUnit = ThresholdType.YEARS; //default value

    @Column(name = "threshold_value", nullable = false)
    private Integer thresholdValue = 1;

    @Column(name = "last_trigger_timestamp")
    private Date lastTriggerTimestamp;

    public FreqType getFrequency() {
        return this.frequency;
    }

    public void setFrequency(FreqType frequency) {
        this.frequency = frequency;
    }

    public Boolean getAutoSchedule() {
        return this.autoSchedule;
    }

    public void setAutoSchedule(Boolean autoSchedule) {
        this.autoSchedule = autoSchedule;
    }

    public ThresholdType getThresholdUnit() {
        return this.thresholdUnit;
    }

    public void setThresholdUnit(ThresholdType thresholdUnit) {
        this.thresholdUnit = thresholdUnit;
    }

    public Integer getThresholdValue() {
        return this.thresholdValue;
    }

    public void setThresholdValue(Integer thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public Date getLastTriggerTimestamp() {
        return this.lastTriggerTimestamp;
    }

    public void setLastTriggerTimestamp(Date lastTriggerTimestamp) {
        this.lastTriggerTimestamp = lastTriggerTimestamp;
    }

    @Override
    public String toString() {
        return "JobsArchive [frequency=" + this.frequency + ", autoSchedule="
                + this.autoSchedule + ", thresholdUnit=" + this.thresholdUnit
                + ", thresholdValue=" + this.thresholdValue
                + ", lastTriggerTimepstamp=" + this.lastTriggerTimestamp
                + ", getId()=" + getId() + "]";
    }
}
