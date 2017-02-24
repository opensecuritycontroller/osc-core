package org.osc.core.broker.model.entities.events;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.osc.core.broker.model.entities.BaseEntity;

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
        return enabled;
    }

    public void setEnable(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getRegexMatch() {
        return regexMatch;
    }

    public void setRegexMatch(String regexMatch) {
        this.regexMatch = regexMatch;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public AlarmAction getAlarmAction() {
        return alarmAction;
    }

    public void setAlarmAction(AlarmAction alarmAction) {
        this.alarmAction = alarmAction;
    }

    public String getReceipientEmail() {
        return receipientEmail;
    }

    public void setReceipientEmail(String receipientEmail) {
        this.receipientEmail = receipientEmail;
    }

    @Override
    public String toString() {
        return "Alarm [enabled=" + enabled + ", name=" + name + ", eventType=" + eventType + ", regexMatch="
                + regexMatch + ", severity=" + severity + ", alarmAction=" + alarmAction + ", receipientEmail="
                + receipientEmail + ", getId()=" + getId() + "]";
    }
}
