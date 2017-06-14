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

public enum SystemFailureType {
    EMAIL_FAILURE("Email Failure"),
    ARCHIVE_FAILURE("Archive Failure"),
    MGR_WEB_SOCKET_NOTIFICATION_FAILURE("Manager Web Socket Notification Failure"),
    OS_NOTIFICATION_FAILURE("Openstack Notification Failure"),
    SCHEDULER_FAILURE("Scheduler Failure"),
    SYSTEM_CLOCK("System Clock Changed"),
    MGR_PROPAGATION_JOB_NOTIFCATION("Manager File Propagation Notification Failure");

    private final String text;

    private SystemFailureType(final String text) {
        this.text = text;
    }

    public static SystemFailureType fromText(String text) {
        for (SystemFailureType type : SystemFailureType.values()) {
            if (type.getText().equals(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "No enum constant " + SystemFailureType.class.getCanonicalName() + " Found for " + text);
    }

    private String getText() {
        return this.text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
