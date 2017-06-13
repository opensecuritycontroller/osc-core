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
package org.osc.core.broker.service.dto.job;

public class ObjectTypeDto {
    public static final ObjectTypeDto VIRTUALIZATION_CONNECTOR = new ObjectTypeDto(
            "VIRTUALIZATION_CONNECTOR", "Virtualization Connector");

    public static final ObjectTypeDto SECURITY_GROUP = new ObjectTypeDto(
            "SECURITY_GROUP", "Security Group");

    public static final ObjectTypeDto APPLIANCE_MANAGER_CONNECTOR = new ObjectTypeDto(
            "APPLIANCE_MANAGER_CONNECTOR", "Manager Connector");

    public static final ObjectTypeDto VIRTUAL_SYSTEM = new ObjectTypeDto(
            "VIRTUAL_SYSTEM", "Virtual System");

    private String name;
    private String displayText;

    public ObjectTypeDto() { }

    public ObjectTypeDto(String name, String displayText) {
        setName(name);
        setDisplayText(displayText);
    }

    @Override
    public String toString() {
        return getDisplayText();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayText() {
        return this.displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }
}