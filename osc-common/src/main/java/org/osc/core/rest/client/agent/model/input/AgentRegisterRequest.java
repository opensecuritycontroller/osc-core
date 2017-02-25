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
package org.osc.core.rest.client.agent.model.input;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.rest.client.agent.model.output.AgentDpaInfo;
import org.osc.core.util.VersionUtil.Version;

import io.swagger.annotations.ApiModelProperty;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentRegisterRequest {

    // Since the name of the getter is different from the field, annotation goes on the getter.
    private Long virtualSystemId;

    @ApiModelProperty(value = "The management interface IP of the appliance", required = true)
    private String applianceIp;

    @ApiModelProperty(value = "The management interface gateway of the appliance", required = true)
    private String applianceGateway;

    @ApiModelProperty(value = "The management interface subnet prefix length.", required = true)
    private String applianceSubnetMask;

    private String applianceName;

    private Version version;

    @ApiModelProperty(value = "Indicates whether the instance is authenticated with its manager.",
            required = true)
    private boolean discovered;

    @ApiModelProperty(value = "Indicates whether the instance is ready to inspect and handle traffic.",
            required = true)
    private boolean inspectionReady;

    @ApiModelProperty
    private Date currentServerTime;

    @ApiModelProperty(value= "The uptime of CPA. Required for agent based deployment.")
    private Long cpaUptime;

    @ApiModelProperty(value= "The pid of CPA. Required for agent based deployment.")
    private String cpaPid;

    @ApiModelProperty(value= "The DPA information. Required for agent based deployment.")
    private AgentDpaInfo agentDpaInfo;

    @ApiModelProperty(name = "applianceName",
            value = "The name of the appliance. This is the name assigned by the controller. It is immutable and unique.",
            required = true)
    public String getName() {
        return this.applianceName;
    }

    public void setName(String name) {
        this.applianceName = name;
    }

    @ApiModelProperty(name="virtualSystemId", value = "The Virtual System Id", required = true)
    public Long getVsId() {
        return this.virtualSystemId;
    }

    public void setVirtualSystemId(Long virtualSystemId) {
        this.virtualSystemId = virtualSystemId;
    }

    public String getApplianceIp() {
        return this.applianceIp;
    }

    public void setApplianceIp(String applianceIpAddress) {
        this.applianceIp = applianceIpAddress;
    }

    @ApiModelProperty(name = "version", value = "The version of CPA. Required for agent based deployment.")
    public Version getAgentVersion() {
        return this.version;
    }

    public void setAgentVersion(Version agentVersion) {
        this.version = agentVersion;
    }

    public boolean isDiscovered() {
        return this.discovered;
    }

    public void setDiscovered(boolean discovered) {
        this.discovered = discovered;
    }

    public boolean isInspectionReady() {
        return this.inspectionReady;
    }

    public void setInspectionReady(boolean inspectionReady) {
        this.inspectionReady = inspectionReady;
    }

    public Date getCurrentServerTime() {
        return this.currentServerTime;
    }

    public void setCurrentServerTime(Date currentServerTime) {
        this.currentServerTime = currentServerTime;
    }

    public Long getCpaUptime() {
        return this.cpaUptime;
    }

    public void setCpaUptime(Long cpaUptime) {
        this.cpaUptime = cpaUptime;
    }

    public String getCpaPid() {
        return this.cpaPid;
    }

    public void setCpaPid(String cpaPid) {
        this.cpaPid = cpaPid;
    }

    public AgentDpaInfo getAgentDpaInfo() {
        return this.agentDpaInfo;
    }

    public void setAgentDpaInfo(AgentDpaInfo agentDpaInfo) {
        this.agentDpaInfo = agentDpaInfo;
    }

    public String getApplianceGateway() {
        return this.applianceGateway;
    }

    public void setApplianceGateway(String mgmtGateway) {
        this.applianceGateway = mgmtGateway;
    }

    public String getApplianceSubnetMask() {
        return this.applianceSubnetMask;
    }

    public void setApplianceSubnetMask(String mgmtSubNetMask) {
        this.applianceSubnetMask = mgmtSubNetMask;
    }

    @Override
    public String toString() {
        return "AgentRegisterRequest [virtualSystemId=" + this.virtualSystemId + ", applianceIp=" + this.applianceIp
                + ", applianceName=" + this.applianceName + ", version=" + this.version + ", discovered="
                + this.discovered + ", inspectionReady=" + this.inspectionReady + ", mgmtGateway="
                + this.applianceGateway + ", mgmtSubnetMask=" + this.applianceSubnetMask + ", currentServerTime="
                + this.currentServerTime + ", cpaUptime=" + this.cpaUptime + ", cpaPid=" + this.cpaPid
                + ", agentDpaInfo=" + this.agentDpaInfo + "]";
    }

}
