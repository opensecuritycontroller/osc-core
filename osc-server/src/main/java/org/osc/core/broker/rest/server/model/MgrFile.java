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
package org.osc.core.broker.rest.server.model;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import io.swagger.annotations.ApiModelProperty;

@XmlRootElement(name = "mgrFile")
@XmlAccessorType(XmlAccessType.FIELD)
public class MgrFile {

    @ApiModelProperty(required=true, value="A stream of bytes represent the content of the file.")
    private byte[] mgrFile = null;

    @ApiModelProperty(required=true, value="The filename will be used when file is persisted.")
    private String mgrFileName = null;

    @ApiModelProperty(value="list of dai IDs, null or empty list will indicate ALL option")
    @XmlElement(name = "applianceInstances")
    private Set<String> applianceInstances = null;

    public Set<String> getApplianceInstances() {
        return this.applianceInstances;
    }

    public void setApplianceInstances(Set<String> applianceInstances) {
        this.applianceInstances = applianceInstances;
    }

    public byte[] getMgrFile() {
        return this.mgrFile;
    }

    public void setMgrfile(byte[] mgrFile) {
        this.mgrFile = mgrFile;
    }

    public String getMgrFileName() {
        return this.mgrFileName;
    }

    public void setMgrFileName(String mgrFileName) {
        this.mgrFileName = mgrFileName;
    }

}
