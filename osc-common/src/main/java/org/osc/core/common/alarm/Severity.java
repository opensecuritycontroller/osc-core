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
package org.osc.core.common.alarm;

public enum Severity {

    HIGH("High"), MEDIUM("Medium"), LOW("Low");

    private final String text;

    private Severity(final String text) {
        this.text = text;
    }

    public static Severity fromText(String text) {
        for (Severity severity : Severity.values()) {
            if (severity.getText().equals(text)) {
                return severity;
            }
        }
        throw new IllegalArgumentException("No enum constant " + Severity.class.getCanonicalName() + " Found for "
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
