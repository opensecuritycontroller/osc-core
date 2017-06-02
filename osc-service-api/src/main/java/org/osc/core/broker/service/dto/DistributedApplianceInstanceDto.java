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

import io.swagger.annotations.ApiModelProperty;

@XmlRootElement(name = "distributedApplianceInstance")
@XmlAccessorType(XmlAccessType.FIELD)
public class DistributedApplianceInstanceDto extends BaseDto {

    @ApiModelProperty(required = true, readOnly = true)
    private Long virtualsystemId;

    @ApiModelProperty(value = "The Manager Connector Id", required = true, readOnly = true)
    private Long mcId;

    @ApiModelProperty(value = "The Virtualization Connector Id", required = true, readOnly = true)
    private Long vcId;

    @ApiModelProperty(value = "The Distributed Appliance Instance Name", required = true, readOnly = true)
    private String name;

    @ApiModelProperty(
            value = "The public Ip address of the instance. This value can be empty till we receive the first callback from the instance.",
            readOnly = true)
    private String ipAddress;

    @ApiModelProperty(required = true, readOnly = true)
    private String applianceModel;

    @ApiModelProperty(required = true, readOnly = true)
    private String swVersion;

    @ApiModelProperty(required = true, readOnly = true)
    private String distributedApplianceName;

    @ApiModelProperty(required = true, readOnly = true)
    private String applianceManagerConnectorName;

    @ApiModelProperty(required = true, readOnly = true)
    private String virtualConnectorName;

    @ApiModelProperty(readOnly = true)
    private String hostname;

    @ApiModelProperty(
            value = "The last time a callback was made from the appliance. If appliance instance status is not supported by the manager it will return 'N/A'.",
            readOnly = true)
    private String lastStatus;

    @ApiModelProperty(value = "Indicates whether the instance is authenticated with its manager. If appliance instance status is not supported by the manager it will return 'N/A'",
            required = true,
            readOnly = true)
    private String discovered;

    @ApiModelProperty(value = "Indicates whether the instance is ready to inspect and handle traffic. If appliance instance status is not supported by the manager it will return 'N/A'",
            required = true,
            readOnly = true)
    private String inspectionReady;

    @ApiModelProperty(value = "The Id of the corresponding server instance on openstack.(Openstack Only)",
            readOnly = true)
    private String osVmId;

    @ApiModelProperty(
            value = "The Hypervisor Host name where the server instance is running on openstack.(Openstack Only)",
            readOnly = true)
    private String osHostname;

    @ApiModelProperty(
            value = "The Inspection port Id of the corresponding server instance in openstack.(Openstack Only)",
            readOnly = true)
    private String osInspectionIngressPortId;

    @ApiModelProperty(
            value = "The Inspection port Mac Address of the corresponding server instance in openstack.(Openstack Only)",
            readOnly = true)
    private String osInspectionIngressMacAddress;

    @ApiModelProperty(
            value = "The Inspection port Id of the corresponding server instance in openstack.(Openstack Only)",
            readOnly = true)
    private String osInspectionEgressPortId;

    @ApiModelProperty(
            value = "The Inspection port Mac Address of the corresponding server instance in openstack.(Openstack Only)",
            readOnly = true)
    private String osInspectionEgressMacAddress;

    @ApiModelProperty(
            value = "The Ip address of the management port of this instance. This could be the same as the ip address of the instance or differnt in case of NAT environments",
            readOnly = true)
    private String mgmtIpAddress;

    @ApiModelProperty(value = "The management port subnet prefix length.", readOnly = true)
    private String mgmtSubnetPrefixLength;

    @ApiModelProperty(value = "The management port gateway.", readOnly = true)
    private String mgmtGateway;

    @ApiModelProperty(readOnly = true, value = "Determines whether the appliance manager enables retrieval of appliance status.")
    private Boolean isApplianceStatusEnabled;

    public DistributedApplianceInstanceDto() {

    }

    public DistributedApplianceInstanceDto(Boolean isApplianceStatusEnabled,
            String discovered, String inspectionReady, String lastStatus) {
        this.isApplianceStatusEnabled = isApplianceStatusEnabled;
        this.discovered = discovered;
        this.inspectionReady = inspectionReady;
        this.lastStatus = lastStatus;
    }



    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getApplianceModel() {
        return this.applianceModel;
    }

    public void setApplianceModel(String applianceModel) {
        this.applianceModel = applianceModel;
    }

    public String getSwVersion() {
        return this.swVersion;
    }

    public void setSwVersion(String swVersion) {
        this.swVersion = swVersion;
    }

    public Boolean isApplianceStatusEnabled() {
        return this.isApplianceStatusEnabled;
    }

    public String getDistributedApplianceName() {
        return this.distributedApplianceName;
    }

    public void setDistributedApplianceName(String distributedApplianceName) {
        this.distributedApplianceName = distributedApplianceName;
    }

    public String getApplianceManagerConnectorName() {
        return this.applianceManagerConnectorName;
    }

    public void setApplianceManagerConnectorName(String applianceManagerConnectorName) {
        this.applianceManagerConnectorName = applianceManagerConnectorName;
    }

    public String getVirtualConnectorName() {
        return this.virtualConnectorName;
    }

    public void setVirtualConnectorName(String virtualConnectorName) {
        this.virtualConnectorName = virtualConnectorName;
    }

    public String getHostname() {
        return this.hostname;
    }

    public String getLastStatus() {
        return this.lastStatus;
    }

    public String getDiscovered() {
        return this.discovered;
    }

    public String getInspectionReady() {
        return this.inspectionReady;
    }

    public String getOsVmId() {
        return this.osVmId;
    }

    public void setOsVmId(String osVmId) {
        this.osVmId = osVmId;
    }

    public String getOsHostname() {
        return this.osHostname;
    }

    public void setOsHostname(String osHostname) {
        this.osHostname = osHostname;
    }

    public String getOsInspectionIngressPortId() {
        return this.osInspectionIngressPortId;
    }

    public void setOsInspectionIngressPortId(String osInspectionPortId) {
        this.osInspectionIngressPortId = osInspectionPortId;
    }

    public String getOsInspectionIngressMacAddress() {
        return this.osInspectionIngressMacAddress;
    }

    public void setOsInspectionIngressMacAddress(String osInspectionMacAddress) {
        this.osInspectionIngressMacAddress = osInspectionMacAddress;
    }

    public String getOsInspectionEgressPortId() {
        return this.osInspectionEgressPortId;
    }

    public void setOsInspectionEgressPortId(String osInspectionEgressPortId) {
        this.osInspectionEgressPortId = osInspectionEgressPortId;
    }

    public String getOsInspectionEgressMacAddress() {
        return this.osInspectionEgressMacAddress;
    }

    public void setOsInspectionEgressMacAddress(String osInspectionEgressMacAddress) {
        this.osInspectionEgressMacAddress = osInspectionEgressMacAddress;
    }

    public Long getVirtualsystemId() {
        return this.virtualsystemId;
    }

    public void setVirtualsystemId(Long virtualsystemId) {
        this.virtualsystemId = virtualsystemId;
    }


    @Override
    public String toString() {
        return "DistributedApplianceInstanceDto [virtualsystemId=" + this.virtualsystemId + ", mcId=" + this.mcId
                + ", vcId=" + this.vcId + ", name=" + this.name + ", ipAddress=" + this.ipAddress + ", applianceModel="
                + this.applianceModel + ", swVersion=" + this.swVersion + ", distributedApplianceName="
                + this.distributedApplianceName + ", applianceManagerConnectorName="
                + this.applianceManagerConnectorName + ", virtualConnectorName=" + this.virtualConnectorName
                + ", hostname=" + this.hostname + ", lastStatus="
                + this.lastStatus + ", discovered=" + this.discovered + ", inspectionReady=" + this.inspectionReady
                + ", osVmId=" + this.osVmId + ", osHostname=" + this.osHostname + ", osInspectionIngressPortId="
                + this.osInspectionIngressPortId + ", osInspectionIngressMacAddress="
                + this.osInspectionIngressMacAddress + ", osInspectionEgressPortId=" + this.osInspectionEgressPortId
                + ", osInspectionEgressMacAddress=" + this.osInspectionEgressMacAddress + ", mgmtIpAddress="
                + this.mgmtIpAddress + ", mgmtSubnetPrefixLength=" + this.mgmtSubnetPrefixLength + ", mgmtGateway="
                + this.mgmtGateway + "]";
    }

    public Long getMcId() {
        return this.mcId;
    }

    public void setMcId(Long mcId) {
        this.mcId = mcId;
    }

    public Long getVcId() {
        return this.vcId;
    }

    public void setVcId(Long vcId) {
        this.vcId = vcId;
    }

    public String getMgmtSubnetPrefixLength() {
        return this.mgmtSubnetPrefixLength;
    }

    public void setMgmtSubnetPrefixLength(String mgmtSubnetPrefixLength) {
        this.mgmtSubnetPrefixLength = mgmtSubnetPrefixLength;
    }

    public String getMgmtGateway() {
        return this.mgmtGateway;
    }

    public void setMgmtGateway(String mgmtGateway) {
        this.mgmtGateway = mgmtGateway;
    }

    public String getMgmtIpAddress() {
        return this.mgmtIpAddress;
    }

    public void setMgmtIpAddress(String mgmtIpAddress) {
        this.mgmtIpAddress = mgmtIpAddress;
    }

}
