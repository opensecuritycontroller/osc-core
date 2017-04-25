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

import java.io.Serializable;
import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.model.entities.archive.FreqType;
import org.osc.core.broker.model.entities.archive.ThresholdType;

@XmlRootElement(name ="jobArchive")
@XmlAccessorType(XmlAccessType.FIELD)
public class JobsArchiveDto extends BaseDto implements Serializable {

    private static final long serialVersionUID = -8266202924666973157L;

    private Boolean autoSchedule;
    private FreqType frequency;
    private ThresholdType thresholdUnit;
    private Integer thresholdValue;
    private Date lastTriggerTimestamp;

	public Boolean getAutoSchedule() {
		return this.autoSchedule;
	}

	public void setAutoSchedule(Boolean autoSchedule) {
		this.autoSchedule = autoSchedule;
	}

	public FreqType getFrequency() {
		return this.frequency;
	}

	public void setFrequency(FreqType frequency) {
		this.frequency = frequency;
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
		return "JobsArchiveDto [autoSchedule=" + this.autoSchedule + ", frequency="
				+ this.frequency + ", thresholdUnit=" + this.thresholdUnit
				+ ", thresholdValue=" + this.thresholdValue
				+ ", lastTriggerTimestamp=" + this.lastTriggerTimestamp
				+ ", getId()=" + getId() + "]";
	}
}
