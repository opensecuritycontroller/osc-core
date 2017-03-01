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
package org.osc.core.rest.client.agent.model.output;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.util.VersionUtil.Version;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentStatusResponse {
    private Version version;
    private Date currentServerTime;
    private Long cpaUptime;
    private String cpaPid;

    private String applianceIp;
    private String applianceGateway;
    private String applianceSubnetMask;
    private Long applianceId;
    private String applianceName;
    private String virtualServerName;
    private String brokerIp;
    private String managerIp;
    private boolean discovered;
    private boolean inspectionReady;

    private List<String> statusLines;
    private AgentDpaInfo agentDpaInfo;
    private String publicIp;

    public Version getVersion() {
        return this.version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public Date getCurrentServerTime() {
        return this.currentServerTime;
    }

    public void setCurrentServerTime(Date currentServerTime) {
        this.currentServerTime = currentServerTime;
    }

    public String getManagerIp() {
        return this.managerIp;
    }

    public void setManagerIp(String managerIp) {
        this.managerIp = managerIp;
    }

    public String getApplianceIp() {
        return this.applianceIp;
    }

    public void setApplianceIp(String applianceIp) {
        this.applianceIp = applianceIp;
    }

    public Long getApplianceId() {
        return this.applianceId;
    }

    public void setApplianceId(Long applianceId) {
        this.applianceId = applianceId;
    }

    public String getApplianceName() {
        return this.applianceName;
    }

    public void setApplianceName(String applianceName) {
        this.applianceName = applianceName;
    }

    public String getCpaPid() {
        return this.cpaPid;
    }

    public void setCpaPid(String cpaPid) {
        this.cpaPid = cpaPid;
    }

    public String getBrokerIp() {
        return this.brokerIp;
    }

    public void setBrokerIp(String brokerIp) {
        this.brokerIp = brokerIp;
    }

    public Long getCpaUptime() {
        return this.cpaUptime;
    }

    public void setCpaUptime(Long cpaUptime) {
        this.cpaUptime = cpaUptime;
    }

    public AgentDpaInfo getAgentDpaInfo() {
        return this.agentDpaInfo;
    }

    public void setAgentDpaInfo(AgentDpaInfo agentDpaInfo) {
        this.agentDpaInfo = agentDpaInfo;
    }

    public List<String> getStatusLines() {
        return this.statusLines;
    }

    public void setStatusLines(List<String> statusLines) {
        this.statusLines = statusLines;
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

    public String getVirtualServer() {
        return this.virtualServerName;
    }

    public void setVirtualServer(String virtualServerName) {
        this.virtualServerName = virtualServerName;
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

    public String getPublicIp() {
        return this.publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    @Override
    public String toString() {
        return "AgentStatusResponse [version=" + this.version + ", currentServerTime=" + this.currentServerTime
                + ", cpaUptime=" + this.cpaUptime + ", cpaPid=" + this.cpaPid + ", applianceIp=" + this.applianceIp
                + ", applianceName=" + this.applianceName + ", virtualServerName=" + this.virtualServerName
                + ", brokerIp=" + this.brokerIp + ", managerIp=" + this.managerIp + ", discovered=" + this.discovered
                + ", inspectionReady=" + this.inspectionReady + ", mgmtGateway=" + this.applianceGateway
                + ", mgmtSubNetMask=" + this.applianceSubnetMask + ", statusLines=" + this.statusLines
                + ", agentDpaInfo=" + this.agentDpaInfo + "]";
    }

}
