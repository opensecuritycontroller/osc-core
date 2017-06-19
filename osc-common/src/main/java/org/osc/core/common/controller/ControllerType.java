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
package org.osc.core.common.controller;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "controllerType")
@XmlAccessorType(XmlAccessType.FIELD)
public class ControllerType {
    private final static String NONE_NAME = "NONE";

    public final static ControllerType NONE = new ControllerType(NONE_NAME);

    private static Set<String> controllerTypes = new HashSet<>(Arrays.asList(NONE_NAME));

    private final String value;

    public ControllerType() {
        this.value = null;
    }

    private ControllerType(final String value) {
        this.value = value;
    }

    public static ControllerType fromText(String text) {
        if (!controllerTypes.contains(text)) {
            throw new IllegalArgumentException("No SDN Controller plugin found for '" + text + "'.");
        }
        return new ControllerType(text);
    }

    /* only used from test {@code VirtualizationConnectorServiceData} */
    public static void addType(String type) {
        controllerTypes.add(type);
    }

    public static void setTypes(Set<String> types) {
        controllerTypes.clear();
        controllerTypes.addAll(types);
    }

    public static Set<String> values() {
        return new TreeSet<>(controllerTypes);
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
        int hash = 1;
        hash = hash * 31 + (getValue() == null ? 0 : getValue().hashCode());
        return hash;
    }
}
