/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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

public enum EventType {
    JOB_FAILURE("Job Failure"),
    SYSTEM_FAILURE("System Failure"),
    DAI_FAILURE("DAI Failure");

    private final String text;

    private EventType(final String text) {
        this.text = text;
    }

    public static EventType fromText(String text) {
        for (EventType type : EventType.values()) {
            if (type.getText().equals(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum constant " + EventType.class.getCanonicalName() + " Found for "
                + text);
    }

    private String getText() {
        return this.text;
    }

    @Override
    public String toString() {
        return text;
    }
}
