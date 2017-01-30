package org.osc.core.broker.rest.server.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import io.swagger.annotations.ApiModelProperty;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Notification {

    public static class EventNotification {

        @ApiModelProperty(value = "Notification Object Identifier. This will be interpreted by the Manager Plugin",
                required = true)
        public String eventObject;
        @ApiModelProperty(value = "Notification Object Type. This will be interpreted by the Manager Plugin",
                required = true)
        public String eventType;

        @Override
        public String toString() {
            return "EventNotification [eventObject=" + this.eventObject + ", eventType=" + this.eventType + "]";
        }
    }

    public EventNotification eventNotification;

    @Override
    public String toString() {
        return "EventNotification [eventNotification=" + this.eventNotification + "]";
    }
}
