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
package org.osc.core.broker.model.entities.appliance;

import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;

@Entity
@Table(name = "DISTRIBUTED_APPLIANCE_INSTANCE")
public class DistributedApplianceInstance extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "virtual_system_fk", nullable = false,
    foreignKey = @ForeignKey(name = "FK_DAI_VIRTUAL_SYSTEM"))
    private VirtualSystem virtualSystem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "deployment_spec_fk", nullable = true,
    foreignKey = @ForeignKey(name = "FK_DAI_DEPLOYMENT_SPEC"))
    private DeploymentSpec deploymentSpec;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "last_status")
    private Date lastStatus;

    @Column(name = "mgr_device_id")
    private String mgrDeviceId;

    @Column(name = "workload_interfaces")
    private Long workloadInterfaces;
    @Column(name = "packets")
    private Long packets;

    @Column(name = "discovered")
    private Boolean discovered;
    @Column(name = "inspection_ready")
    private Boolean inspectionReady;

    @Column(name = "os_host_name")
    private String osHostName;
    @Column(name = "os_availability_zone_name")
    private String osAvailabilityZone;
    @Column(name = "external_id")
    private String externalId;

    @Column(name = "inspection_element_id")
    private String inspectionElementId;
    @Column(name = "inspection_element_parent_id")
    private String inspectionElementParentId;

    @Column(name = "inspection_os_ingress_port_id")
    private String inspectionOsIngressPortId;
    @Column(name = "inspection_ingress_mac_address")
    private String inspectionIngressMacAddress;

    @Column(name = "inspection_os_egress_port_id")
    private String inspectionOsEgressPortId;
    @Column(name = "inspection_egress_mac_address")
    private String inspectionEgressMacAddress;

    @Column(name = "mgmt_os_port_id")
    private String mgmtOsPortId;
    @Column(name = "mgmt_mac_address")
    private String mgmtMacAddress;

    @Column(name = "floating_ip_id")
    private String floatingIpId;

    @Column(name = "mgmt_ip_address")
    private String mgmtIpAddress;
    @Column(name = "mgmt_gateway_address")
    private String mgmtGateway;
    @Column(name = "mgmt_subnet_prefix_length")
    private String mgmtSubnetPrefixLength;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "DISTRIBUTED_APPLIANCE_INSTANCE_VM_PORT",
    joinColumns=
    @JoinColumn(name="dai_fk", referencedColumnName="id"),
    inverseJoinColumns=
    @JoinColumn(name="vm_port_fk", referencedColumnName="id")
            )
    private Set<VMPort> protectedPorts = new HashSet<>();

    @Column(name = "current_console_password")
    private String currentConsolePassword;
    @Column(name = "new_console_password")
    private String newConsolePassword;

    @Column(name = "appliance_config")
    @Basic(fetch = FetchType.LAZY)
    @Lob
    private byte[] applianceConfig;

    public DistributedApplianceInstance() { // needed by Hibernate dynamic query
        super();
    }

    public DistributedApplianceInstance(VirtualSystem virtualSystem) {
        super();
        this.virtualSystem = virtualSystem;
    }


    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VirtualSystem getVirtualSystem() {
        return this.virtualSystem;
    }

    void setVirtualSystem(VirtualSystem virtualSystem) {
        this.virtualSystem = virtualSystem;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getInspectionElementId() {
        return this.inspectionElementId;
    }

    public void setInspectionElementId(String inspectionElementId) {
        this.inspectionElementId = inspectionElementId;
    }

    public String getInspectionElementParentId() {
        return this.inspectionElementParentId;
    }

    public void setInspectionElementParentId(String inspectionElementParentId) {
        this.inspectionElementParentId = inspectionElementParentId;
    }

    public Date getLastStatus() {
        return this.lastStatus;
    }

    public void setLastStatus(Date lastStatus) {
        this.lastStatus = lastStatus;
    }

    public String getMgrDeviceId() {
        return this.mgrDeviceId;
    }

    public void setMgrDeviceId(String mgrDeviceId) {
        this.mgrDeviceId = mgrDeviceId;
    }

    public Long getWorkloadInterfaces() {
        return this.workloadInterfaces;
    }

    public void setWorkloadInterfaces(Long workloadInterfaces) {
        this.workloadInterfaces = workloadInterfaces;
    }

    public Long getPackets() {
        return this.packets;
    }

    public void setPackets(Long packets) {
        this.packets = packets;
    }

    public void setNewConsolePassword(String newConsolePassword) {
        this.newConsolePassword = newConsolePassword;
    }

    public Boolean getDiscovered() {
        return this.discovered;
    }

    public void setDiscovered(Boolean discovered) {
        this.discovered = discovered;
    }

    public Boolean getInspectionReady() {
        return this.inspectionReady;
    }

    public void setInspectionReady(Boolean inspectionReady) {
        this.inspectionReady = inspectionReady;
    }

    public String getOsHostName() {
        return this.osHostName;
    }

    public void setOsHostName(String osHostName) {
        this.osHostName = osHostName;
    }

    public DeploymentSpec getDeploymentSpec() {
        return this.deploymentSpec;
    }

    public void setDeploymentSpec(DeploymentSpec deploymentSpec) {
        this.deploymentSpec = deploymentSpec;
    }

    public String getExternalId() {
        return this.externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getOsAvailabilityZone() {
        return this.osAvailabilityZone;
    }

    public void setOsAvailabilityZone(String osAvailabilityZone) {
        this.osAvailabilityZone = osAvailabilityZone;
    }

    /**
     * Returns the host name. For OpenStack, returns the hypervisor hostname.
     */
    public String getHostName() {
        return this.osHostName;
    }

    public byte[] getApplianceConfig() {
        return this.applianceConfig;
    }

    public void setApplianceConfig(byte[] applianceConfig) {
        this.applianceConfig = applianceConfig;
    }

    public String getInspectionOsIngressPortId() {
        return this.inspectionOsIngressPortId;
    }

    public void setInspectionOsIngressPortId(String inspectionOsPortId) {
        this.inspectionOsIngressPortId = inspectionOsPortId;
    }

    public String getMgmtOsPortId() {
        return this.mgmtOsPortId;
    }

    public void setMgmtOsPortId(String mgmtOsPortId) {
        this.mgmtOsPortId = mgmtOsPortId;
    }

    public String getMgmtMacAddress() {
        return this.mgmtMacAddress;
    }

    public void setMgmtMacAddress(String mgmtMacAddress) {
        this.mgmtMacAddress = mgmtMacAddress;
    }

    public String getInspectionIngressMacAddress() {
        return this.inspectionIngressMacAddress;
    }

    public void setInspectionIngressMacAddress(String inspectionMacAddress) {
        this.inspectionIngressMacAddress = inspectionMacAddress;
    }

    public String getFloatingIpId() {
        return this.floatingIpId;
    }

    public void setFloatingIpId(String floatingIpId) {
        this.floatingIpId = floatingIpId;
    }

    public Set<VMPort> getProtectedPorts() {
        return this.protectedPorts;
    }

    public void addProtectedPort(VMPort protectedPort) {
        this.protectedPorts.add(protectedPort);
    }

    public void removeProtectedPort(VMPort protectedPort) {
        this.protectedPorts.remove(protectedPort);
    }

    public String getMgmtIpAddress() {
        return this.mgmtIpAddress;
    }

    public void setMgmtIpAddress(String mgmtIpAddress) {
        this.mgmtIpAddress = mgmtIpAddress;
    }

    public String getMgmtGateway() {
        return this.mgmtGateway;
    }

    public void setMgmtGateway(String mgmtGateway) {
        this.mgmtGateway = mgmtGateway;
    }

    public String getMgmtSubnetPrefixLength() {
        return this.mgmtSubnetPrefixLength;
    }

    public void setMgmtSubnetPrefixLength(String mgmtSubnetPrefixLength) {
        this.mgmtSubnetPrefixLength = mgmtSubnetPrefixLength;
    }

    public String getInspectionOsEgressPortId() {
        return this.inspectionOsEgressPortId;
    }

    public void setInspectionOsEgressPortId(String inspectionOsEgressPortId) {
        this.inspectionOsEgressPortId = inspectionOsEgressPortId;
    }

    public String getInspectionEgressMacAddress() {
        return this.inspectionEgressMacAddress;
    }

    public void setInspectionEgressMacAddress(String inspectionEgressMacAddress) {
        this.inspectionEgressMacAddress = inspectionEgressMacAddress;
    }

    public boolean isSingleNicInspection() {
        return Objects.equals(this.inspectionOsIngressPortId, this.inspectionOsEgressPortId)
                && Objects.equals(this.inspectionIngressMacAddress, this.inspectionEgressMacAddress);
    }

    /**
     * Resets all the discovered attributes for the Appliance instance.
     */
    public void resetAllDiscoveredAttributes() {
        this.externalId = null;
        this.inspectionIngressMacAddress = null;
        this.inspectionOsIngressPortId = null;
        this.inspectionEgressMacAddress = null;
        this.inspectionOsEgressPortId = null;
        this.discovered = false;
        this.inspectionReady = false;
    }

    public void updateDaiOpenstackSvaInfo(String serverId,
            String ingressMacAddr, String ingressPortId, String egressMacAddr,
            String egressPortId) {
        this.externalId = serverId;
        this.inspectionIngressMacAddress = ingressMacAddr;
        this.inspectionOsIngressPortId = ingressPortId;
        this.inspectionEgressMacAddress = egressMacAddr;
        this.inspectionOsEgressPortId = egressPortId;
    }
}