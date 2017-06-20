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
