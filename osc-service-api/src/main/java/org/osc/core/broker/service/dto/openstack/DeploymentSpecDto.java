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
package org.osc.core.broker.service.dto.openstack;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.service.dto.BaseDto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Parent Id is applicable for this object. The corresponding Virtual System is considered"
        + " the parent of this Deployment Specification.<br/>"
        + "Once a deployment specification is created, all the fields are read only unless indicated.")
@XmlRootElement(name = "deploymentSpec")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeploymentSpecDto extends BaseDto {

    @ApiModelProperty(value = "The Deployment Specification name", required = true, readOnly = true)
    private String name;

    @ApiModelProperty(value = "The id of the project on behalf of which the service function instances will be deployed",
            readOnly = true)
    private String projectId;

    @ApiModelProperty(value = "The virtualization environment namespace for the entities created by this deployment. For instance, a Kubernetes namespace.")
    private String namespace;

    @ApiModelProperty(
            value = "The name of the project on behalf of which the service function instances will be deployed",
            readOnly = true)
    private String projectName;

    @ApiModelProperty(value = "The region to which the service function instances will be deployed",
            readOnly = true)
    private String region;

    @ApiModelProperty(
            value = "The name of the management network which the service function instances will be connected to",
            readOnly = true)
    private String managementNetworkName;

    @ApiModelProperty(value = "The id of management network which the service function instances will be connected to",
            readOnly = true)
    private String managementNetworkId;

    @ApiModelProperty(
            value = "The name of the inspection network which the service function instances will be connected to",
            readOnly = true)
    private String inspectionNetworkName;

    @ApiModelProperty(value = "The id of inspection network which the service function instances will be connected to",
            readOnly = true)
    private String inspectionNetworkId;

    @ApiModelProperty(value = "The port group under which all the distributed appliance instances are registered under.",
            readOnly = true)
    private String portGroupId;

    @ApiModelProperty(
            value = "The floating ip pool from which floating ips will be allocated in case of NAT'ed environments",
            readOnly = true)
    private String floatingIpPoolName;

    @ApiModelProperty(value = "The Availablity zones to deploy instances to", readOnly = true)
    private Set<AvailabilityZoneDto> availabilityZones = new HashSet<AvailabilityZoneDto>();

    @ApiModelProperty(value = "The hosts to deploy instances to", readOnly = true)
    private Set<HostDto> hosts = new HashSet<HostDto>();

    @ApiModelProperty(value = "The Host Aggregates to deploy instances to", readOnly = true)
    private Set<HostAggregateDto> hostAggregates = new HashSet<HostAggregateDto>();

    @ApiModelProperty(
            value = "The number of instances to deploy. This is applicable only for host based deployment, for all other deployments count is expected to be 1",
            required = true,
            readOnly = true)
    private Integer count;

    @ApiModelProperty(
            value = "Indicates whether the Deployment specification is exclusive for that project or if its shared across projects",
            required = true,
            readOnly = true)
    private boolean isShared;

    @ApiModelProperty(value = "Indicates whether the deployment specification is marked for deletion",
            required = true,
            readOnly = true)
    private boolean markForDeletion = false;

    @ApiModelProperty(readOnly = true)
    private String lastJobState;

    @ApiModelProperty(readOnly = true)
    private String lastJobStatus;

    @ApiModelProperty(readOnly = true)
    private Long lastJobId;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProjectId() {
        return this.projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return this.projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getRegion() {
        return this.region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getManagementNetworkName() {
        return this.managementNetworkName;
    }

    public void setManagementNetworkName(String managementNetworkName) {
        this.managementNetworkName = managementNetworkName;
    }

    public String getManagementNetworkId() {
        return this.managementNetworkId;
    }

    public void setManagementNetworkId(String managementNetworkId) {
        this.managementNetworkId = managementNetworkId;
    }

    public Set<AvailabilityZoneDto> getAvailabilityZones() {
        return this.availabilityZones;
    }

    public void setAvailabilityZones(Set<AvailabilityZoneDto> availabilityZones) {
        this.availabilityZones = availabilityZones;
    }

    public Set<HostDto> getHosts() {
        return this.hosts;
    }

    public void setHosts(Set<HostDto> hosts) {
        this.hosts = hosts;
    }

    public Integer getCount() {
        return this.count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Set<HostAggregateDto> getHostAggregates() {
        return this.hostAggregates;
    }

    public void setHostAggregates(Set<HostAggregateDto> hostAggregates) {
        this.hostAggregates = hostAggregates;
    }

    public boolean isMarkForDeletion() {
        return this.markForDeletion;
    }

    public void setMarkForDeletion(boolean markForDeletion) {
        this.markForDeletion = markForDeletion;
    }

    public boolean isShared() {
        return this.isShared;
    }

    public void setShared(boolean isShared) {
        this.isShared = isShared;
    }

    public String getInspectionNetworkName() {
        return this.inspectionNetworkName;
    }

    public void setInspectionNetworkName(String inspectionNetworkName) {
        this.inspectionNetworkName = inspectionNetworkName;
    }

    public String getInspectionNetworkId() {
        return this.inspectionNetworkId;
    }

    public void setInspectionNetworkId(String inspectionNetworkId) {
        this.inspectionNetworkId = inspectionNetworkId;
    }

    public String getPortGroupId() {
        return this.portGroupId;
    }

    public void setPortGroupId(String portGroupId) {
        this.portGroupId = portGroupId;
    }

    public String getFloatingIpPoolName() {
        return this.floatingIpPoolName;
    }

    public void setFloatingIpPoolName(String floatingIpPoolName) {
        this.floatingIpPoolName = floatingIpPoolName;
    }

    public String getLastJobState() {
        return this.lastJobState;
    }

    public void setLastJobState(String lastJobState) {
        this.lastJobState = lastJobState;
    }

    public String getLastJobStatus() {
        return this.lastJobStatus;
    }

    public void setLastJobStatus(String lastJobStatus) {
        this.lastJobStatus = lastJobStatus;
    }

    public Long getLastJobId() {
        return this.lastJobId;
    }

    public void setLastJobId(Long lastJobId) {
        this.lastJobId = lastJobId;
    }

    public DeploymentSpecDto withParentId(Long id) {
        setParentId(id);
        return this;
    }

    @Override
    public String toString() {
        return "DeploymentSpecDto [name=" + this.name + ", projectId=" + this.projectId + ", projectName="
                + this.projectName + ", region=" + this.region + ", managementNetworkName=" + this.managementNetworkName
                + ", managementNetworkId=" + this.managementNetworkId + ", availabilityZones=" + this.availabilityZones
                + ", hosts=" + this.hosts + ", count=" + this.count + "]";
    }
}
