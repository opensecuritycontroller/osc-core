package org.osc.core.broker.service.dto.openstack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.job.JobState;
import org.osc.core.broker.job.JobStatus;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.util.ValidateUtil;

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

    @ApiModelProperty(value = "The id of the tenant on behalf of which the service function instances will be deployed",
            required = true,
            readOnly = true)
    private String tenantId;

    @ApiModelProperty(
            value = "The name of the tenant on behalf of which the service function instances will be deployed",
            required = true,
            readOnly = true)
    private String tenantName;

    @ApiModelProperty(value = "The region to which the service function instances will be deployed",
            required = true,
            readOnly = true)
    private String region;

    @ApiModelProperty(
            value = "The name of the management network which the service function instances will be connected to",
            required = true,
            readOnly = true)
    private String managementNetworkName;

    @ApiModelProperty(value = "The id of management network which the service function instances will be connected to",
            required = true,
            readOnly = true)
    private String managementNetworkId;

    @ApiModelProperty(
            value = "The name of the inspection network which the service function instances will be connected to",
            required = true,
            readOnly = true)
    private String inspectionNetworkName;

    @ApiModelProperty(value = "The id of inspection network which the service function instances will be connected to",
            required = true,
            readOnly = true)
    private String inspectionNetworkId;

    @ApiModelProperty(
            value = "The floating ip pool from which floating ips will be allocated in case of NAT'ed environemnts",
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
            value = "Indicates whether the Deployment specification is exclusive for that tenant or if its shared across tenants",
            required = true,
            readOnly = true)
    private boolean isShared;

    @ApiModelProperty(value = "Indicates whether the deployment specifcation is marked for deletion",
            required = true,
            readOnly = true)
    private boolean markForDeletion = false;

    @ApiModelProperty(readOnly = true)
    private JobState lastJobState;

    @ApiModelProperty(readOnly = true)
    private JobStatus lastJobStatus;

    @ApiModelProperty(readOnly = true)
    private Long lastJobId;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantName() {
        return this.tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getRegion() {
        return this.region;
    }

    public void setRegion(String region) {
        this.region = region;
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

    public String getFloatingIpPoolName() {
        return this.floatingIpPoolName;
    }

    public void setFloatingIpPoolName(String floatingIpPoolName) {
        this.floatingIpPoolName = floatingIpPoolName;
    }

    public JobState getLastJobState() {
        return this.lastJobState;
    }

    public void setLastJobState(JobState lastJobState) {
        this.lastJobState = lastJobState;
    }

    public JobStatus getLastJobStatus() {
        return this.lastJobStatus;
    }

    public void setLastJobStatus(JobStatus lastJobStatus) {
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
        return "DeploymentSpecDto [name=" + this.name + ", tenantId=" + this.tenantId + ", tenantName="
                + this.tenantName + ", region=" + this.region + ", managementNetworkName=" + this.managementNetworkName
                + ", managementNetworkId=" + this.managementNetworkId + ", availabilityZones=" + this.availabilityZones
                + ", hosts=" + this.hosts + ", count=" + this.count + "]";
    }

    public static void checkForNullFields(DeploymentSpecDto dto) throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("Name", dto.getName());

        map.put("Tenant Name", dto.getTenantName());
        map.put("Tenant", dto.getTenantId());

        map.put("Region", dto.getRegion());

        map.put("Virtual System Id", dto.getParentId());

        map.put("Management Network Name", dto.getManagementNetworkName());
        map.put("Management Network Id", dto.getManagementNetworkId());

        map.put("Inspection Network Name", dto.getInspectionNetworkName());
        map.put("Inspection Network Id", dto.getInspectionNetworkId());

        map.put("Instance Count", dto.getCount());

        ValidateUtil.checkForNullFields(map);
    }

    public static void checkFieldLength(DeploymentSpecDto dto) throws Exception {

        Map<String, String> map = new HashMap<String, String>();

        map.put("Name", dto.getName());
        ValidateUtil.validateFieldLength(map, ValidateUtil.DEFAULT_MAX_LEN);
    }

}
