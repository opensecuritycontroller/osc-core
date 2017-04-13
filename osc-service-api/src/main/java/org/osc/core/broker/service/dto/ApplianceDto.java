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
package org.osc.core.broker.service.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

// Appliance Data Transfer Object associated with Appliance Entity
@XmlRootElement(name = "appliance")
@XmlAccessorType(XmlAccessType.FIELD)
public class ApplianceDto extends BaseDto {

    private String model;
    private String managerType;
    private String managerVersion;

    public String getModel() {
        return this.model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getManagerType() {
        return this.managerType;
    }

    public void setManagerType(String managerType) {
        this.managerType = managerType;
    }

    public String getManagerVersion() {
        return this.managerVersion;
    }

    public void setManagerVersion(String managerVersion) {
        this.managerVersion = managerVersion;
    }

    @Override
    public String toString() {
        return "ApplianceDto [id=" + getId() + ", model=" + this.model + ", managerType=" + this.managerType + ", managerVersion="
                + this.managerVersion + "]";
    }
}
