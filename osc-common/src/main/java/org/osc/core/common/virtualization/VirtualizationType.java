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
package org.osc.core.common.virtualization;


public enum VirtualizationType {
    OPENSTACK("OPENSTACK"), KUBERNETES("KUBERNETES");

    private final String text;

    private VirtualizationType(final String text) {
        this.text = text;
    }

    public static VirtualizationType fromText(String text) {
        for (VirtualizationType type : VirtualizationType.values()) {
            if (type.getText().equals(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum constant " + VirtualizationType.class.getCanonicalName() + " Found for " + text);
    }

    private String getText() {
        return this.text;
    }

    public boolean isOpenstack() {
        return equals(VirtualizationType.OPENSTACK);
    }


    public boolean isKubernetes() {
        return equals(VirtualizationType.KUBERNETES);
    }

    @Override
    public String toString() {
        return this.text;
    }
}
