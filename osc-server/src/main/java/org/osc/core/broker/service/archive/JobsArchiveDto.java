package org.osc.core.broker.service.archive;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.model.entities.archive.FreqType;
import org.osc.core.broker.model.entities.archive.ThresholdType;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.util.ValidateUtil;

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
		return autoSchedule;
	}

	public void setAutoSchedule(Boolean autoSchedule) {
		this.autoSchedule = autoSchedule;
	}

	public FreqType getFrequency() {
		return frequency;
	}

	public void setFrequency(FreqType frequency) {
		this.frequency = frequency;
	}

	public ThresholdType getThresholdUnit() {
		return thresholdUnit;
	}

	public void setThresholdUnit(ThresholdType thresholdUnit) {
		this.thresholdUnit = thresholdUnit;
	}

	public Integer getThresholdValue() {
		return thresholdValue;
	}

	public void setThresholdValue(Integer thresholdValue) {
		this.thresholdValue = thresholdValue;
	}

	public Date getLastTriggerTimestamp() {
		return lastTriggerTimestamp;
	}

	public void setLastTriggerTimestamp(Date lastTriggerTimestamp) {
		this.lastTriggerTimestamp = lastTriggerTimestamp;
	}

	public static void checkForNullFields(JobsArchiveDto dto) throws Exception {
        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("frequency", dto.getFrequency());
        map.put("autoSchedule", dto.getAutoSchedule());
        map.put("thresholdUnit", dto.getThresholdUnit());
        map.put("thresholdValue", dto.getThresholdValue());

        ValidateUtil.checkForNullFields(map);
    }

	@Override
	public String toString() {
		return "JobsArchiveDto [autoSchedule=" + autoSchedule + ", frequency="
				+ frequency + ", thresholdUnit=" + thresholdUnit
				+ ", thresholdValue=" + thresholdValue
				+ ", lastTriggerTimestamp=" + lastTriggerTimestamp
				+ ", getId()=" + getId() + "]";
	}
}
