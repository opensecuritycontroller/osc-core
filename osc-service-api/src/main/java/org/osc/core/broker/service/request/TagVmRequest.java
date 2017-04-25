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
package org.osc.core.broker.service.request;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class TagVmRequest implements Request {

    private String applianceInstanceName;
    private String vmUuid;
    private String ipAddress;
    private String tag;

    // TODO emanoel: prepare UnTagVmRequest with separate validator
    public String getApplianceInstanceName() {
        return this.applianceInstanceName;
    }

    public void setApplianceInstanceName(String applianceInstanceName) {
        this.applianceInstanceName = applianceInstanceName;
    }

    public String getVmUuid() {
        return this.vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getTag() {
        return this.tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    @Override
    public String toString() {
        return "TagVmRequest [applianceInstanceName=" + this.applianceInstanceName + ", vmUuid=" + this.vmUuid
                + ", ipAddress=" + this.ipAddress + "]";
    }

}
