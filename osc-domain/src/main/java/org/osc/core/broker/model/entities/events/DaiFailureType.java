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

public enum DaiFailureType {

    DAI_STATUS_CHANGE("DAI Status Change"),
    DAI_TIMEOUT("DAI Timeout"),
    DAI_NSX_STATUS_UPDATE("DAI NSX status update"),
    DAI_MGR_INITIAL_CONFIG("DAI Manager Initial Config"),
    DAI_MGR_CHECK("DAI Manager Check");

    private final String text;

    private DaiFailureType(final String text) {
        this.text = text;
    }

    public static DaiFailureType fromText(String text) {
        for (DaiFailureType type : DaiFailureType.values()) {
            if (type.getText().equals(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "No enum constant " + DaiFailureType.class.getCanonicalName() + " Found for " + text);
    }

    private String getText() {
        return this.text;
    }

    @Override
    public String toString() {
        return text;
    }
}
