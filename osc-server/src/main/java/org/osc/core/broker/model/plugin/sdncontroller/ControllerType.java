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
package org.osc.core.broker.model.plugin.sdncontroller;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.builder.HashCodeBuilder;

import io.swagger.annotations.ApiModelProperty;

@XmlRootElement(name = "controllerType")
@XmlAccessorType(XmlAccessType.FIELD)
public class ControllerType {
    private final static String NONE_NAME = "NONE";

    private static Set<String> controllerTypes = new HashSet<String>(Arrays.asList(NONE_NAME));

    public final static ControllerType NONE = ControllerType.fromText(NONE_NAME);

    @ApiModelProperty(required=true, value="Registered plugin names like NSC, MIDO-NET etc")
    private final String value;

    public ControllerType() {
        this.value = null;
    }

    private ControllerType(final String value) {
        this.value = value;
    }

    public static void addTypes(Set<String> controllerTypes) {
        ControllerType.controllerTypes.addAll(controllerTypes);
    }

    public static void addType(String controllerType) {
        ControllerType.controllerTypes.add(controllerType);
    }

    public static void removeType(String controllerType) {
        ControllerType.controllerTypes.remove(controllerType);
    }

    public static ControllerType fromText(String text) {
        if (!controllerTypes.contains(text)) {
            throw new IllegalArgumentException("No SDN Controller plugin found for '" + text + "'.");
        }
        return new ControllerType(text);
    }

    public static Set<String> values() {
        return controllerTypes;
    }

    public static String valueOf(String value) {
        return ControllerType.fromText(value).getValue();
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
        if (!(other instanceof ControllerType)) {
            return false;
        }
        ControllerType otherControllerType = (ControllerType) other;
        return otherControllerType.getValue().equals(getValue());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getValue()).toHashCode();
    }
}
