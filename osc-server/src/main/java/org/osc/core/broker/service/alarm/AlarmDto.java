package org.osc.core.broker.service.alarm;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.events.AlarmAction;
import org.osc.core.broker.model.entities.events.EventType;
import org.osc.core.broker.model.entities.events.Severity;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.util.ValidateUtil;

import io.swagger.annotations.ApiModelProperty;
import org.osc.core.broker.validator.annotations.CustomAlarmDto;
import org.osc.core.broker.validator.annotations.Email;
import org.osc.core.broker.validator.annotations.Regex;

@XmlRootElement(name = "alarm")
@XmlAccessorType(XmlAccessType.FIELD)
@CustomAlarmDto
public class AlarmDto extends BaseDto {

    private static final Logger log = Logger.getLogger(AlarmDto.class);

    @ApiModelProperty(required = true)
    @NotNull(message = "alarm.enabledAlarm is required")
    private boolean enabledAlarm;

    @ApiModelProperty(required = true)
    @NotNull(message = "alarm.name is required")
    @Size(max = 155,message = "alarm.name is too big")
    private String name;

    @ApiModelProperty(required = true)
    @NotNull(message = "alarm.eventType is required")
    private EventType eventType;

    @ApiModelProperty(required = true)
    @NotNull(message = "alarm.regexMatch is required")
    @Regex(message = "alarm.regexMatch is invalid")
    @Size(max = 155,message = "alarm.regexMatch is too big")
    private String regexMatch;

    @ApiModelProperty(required = true)
    @NotNull(message = "alarm.severity is required")
    private Severity severity;

    @ApiModelProperty(required = true)
    @NotNull(message = "alarm.alarmAction is required")
    private AlarmAction alarmAction;

    @ApiModelProperty(value = "If Alarm action is email, then this field is required.")
    @Size(max = 155,message = "alarm.recipientEmail is too big")
    @Email(message = "alarm.recipientEmail is invalid" )
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

    public static void checkForNullFields(AlarmDto dto) throws Exception {
        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("name", dto.getName());
        map.put("eventType", dto.getEventType());
        map.put("severity", dto.getSeverity());
        map.put("alarmAction", dto.getAlarmAction());
        if (dto.getAlarmAction().equals(AlarmAction.EMAIL)) {
            map.put("email", dto.getReceipientEmail());
        }

        ValidateUtil.checkForNullFields(map);

    }

    public static void checkFieldLength(AlarmDto dto) throws Exception {

        Map<String, String> map = new HashMap<String, String>();

        map.put("name", dto.getName());
        map.put("regexMatch", dto.getRegexMatch());
        map.put("Email", dto.getReceipientEmail());

        ValidateUtil.validateFieldLength(map, ValidateUtil.DEFAULT_MAX_LEN);

    }

    public static void checkRegexSyntax(AlarmDto dto) throws Exception {

        try {
            if (dto.getEventType().equals(EventType.JOB_FAILURE)) {
                Pattern.compile(dto.getRegexMatch());
            }
        } catch (PatternSyntaxException ex) {
            log.error("regexMatch syntax is invalid: " + ex.getMessage());
            throw new VmidcBrokerValidationException("regexMatch: " + dto.getRegexMatch() + " is invalid.");
        }
    }
}
