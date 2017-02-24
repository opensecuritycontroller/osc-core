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
package org.osc.core.broker.model.plugin.manager;

import io.swagger.annotations.ApiModelProperty;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.osc.sdk.manager.element.ManagerTypeElement;

@XmlRootElement(name ="managerType")
@XmlAccessorType(XmlAccessType.FIELD)
public class ManagerType implements ManagerTypeElement {
    public final static String NSM_MANAGER_NAME = "NSM";
    public final static String SMC_MANAGER_NAME = "SMC";

    private static Set<String> managerTypes = new HashSet<String>(Arrays.asList(NSM_MANAGER_NAME, SMC_MANAGER_NAME));

    public final static ManagerType NSM = ManagerType.fromText("NSM");
    public final static ManagerType SMC = ManagerType.fromText("SMC");

    @ApiModelProperty(required=true, value="Registered plugin names like NSM, SMC etc")
    private final String value;

    public ManagerType() {
        this.value = null;
    }

    private ManagerType(final String value) {
        this.value = value;
    }

    public static void addTypes(Set<String> managerTypes) {
        ManagerType.managerTypes.addAll(managerTypes);
    }

    public static void addType(String managerType) {
        ManagerType.managerTypes.add(managerType);
    }

    public static void removeType(String managerType) {
        ManagerType.managerTypes.remove(managerType);
    }

    public static ManagerType fromText(String text) {
        if (!managerTypes.contains(text)) {
            throw new IllegalArgumentException("No manager type found for '" + text + "'");
        }
        return new ManagerType(text);
    }

    public static Set<String> values() {
        return managerTypes;
    }

    public static String valueOf(String value) {
        return ManagerType.fromText(value).getValue();
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (!(other instanceof ManagerType)) {
            return false;
        }
        ManagerType otherManagerType = (ManagerType) other;
        return otherManagerType.getValue().equals(getValue());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getValue()).toHashCode();
    }
}
