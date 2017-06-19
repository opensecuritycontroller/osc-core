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
package org.osc.core.common.job;

public enum AcknowledgementStatus {

    PENDING_ACKNOWLEDGEMENT("Pending Acknowledgement"), ACKNOWLEDGED("Acknowledged");

    private final String text;

    private AcknowledgementStatus(final String text) {
        this.text = text;
    }

    public static AcknowledgementStatus fromText(String text) {
        for (AcknowledgementStatus status : AcknowledgementStatus.values()) {
            if (status.getText().equals(text)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No enum constant " + AcknowledgementStatus.class.getCanonicalName()
                + " Found for " + text);
    }

    private String getText() {
        return this.text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
