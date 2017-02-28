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
package org.osc.core.broker.model.virtualization;


public enum OpenstackSoftwareVersion {
    OS_ICEHOUSE("Icehouse");

    private final String text;

    private OpenstackSoftwareVersion(final String text) {
        this.text = text;
    }

    public static OpenstackSoftwareVersion fromText(String text) {
        for (OpenstackSoftwareVersion version : OpenstackSoftwareVersion.values()) {
            if (version.getText().equals(text)) {
                return version;
            }
        }
        throw new IllegalArgumentException("No enum constant " + OpenstackSoftwareVersion.class.getCanonicalName() + " Found for " + text);
    }

    private String getText() {
        return this.text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
