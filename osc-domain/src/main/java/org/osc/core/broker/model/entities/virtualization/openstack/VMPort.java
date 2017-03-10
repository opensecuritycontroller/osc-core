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
package org.osc.core.broker.model.entities.virtualization.openstack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;

@SuppressWarnings("serial")
@Entity
@Table(name = "VM_PORT")
public class VMPort extends BaseEntity {

    @Column(name = "os_network_id", nullable = false)
    private String osNetworkId;

    @Column(name = "os_port_id", nullable = false, unique = true)
    private String openstackId;

    @Column(name = "mac_address", nullable = false, unique = true)
    private String macAddress;

    @ElementCollection(fetch = FetchType.LAZY)
    @Column(name = "ip_address")
    @CollectionTable(name = "VM_PORT_IP_ADDRESS", joinColumns = @JoinColumn(name = "vm_port_fk"),
            foreignKey=@ForeignKey(name = "FK_VM_PORT_IP_ADDRESS"))
    private List<String> ipAddresses = new ArrayList<String>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vm_fk", foreignKey = @ForeignKey(name = "FK_VMP_VM"))
    private VM vm;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "network_fk", foreignKey = @ForeignKey(name = "FK_VMP_NETWORK"))
    private Network network;

    @Column(name = "parent_id", nullable = true, unique = false)
    private String parentId;

    /*
     * \
     * TODO: Future, Later we want to modify our infrastructure to support multiple subnets per port.
     * Current version have MaytoOne relation. However, once we support multiple IP Addresses per port we need to change
     * This also needs to be documented in release docs
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subnet_fk", foreignKey = @ForeignKey(name = "FK_VMP_SUBNET"))
    private Subnet subnet;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "protectedPorts")
    private Set<DistributedApplianceInstance> dais = new HashSet<DistributedApplianceInstance>();

    public VMPort(VM vm, String macAddress, String osNetworkId, String openstackId, List<String> ipAddresses) {
        this.vm = vm;
        this.osNetworkId = osNetworkId;
        this.openstackId = openstackId;
        this.macAddress = macAddress;
        this.ipAddresses = ipAddresses;
        this.vm.addPort(this);
    }

    public VMPort(Network network, String macAddress, String osNetworkId, String openstackId, List<String> ipAddresses) {
        this.network = network;
        this.osNetworkId = network.getOpenstackId();
        this.openstackId = openstackId;
        this.macAddress = macAddress;
        this.ipAddresses = ipAddresses;
        this.network.addPort(this);
    }

    public VMPort(Subnet subnet, String macAddress, String osNetworkId, String openstackId, List<String> ipAddresses) {
        this.subnet = subnet;
        this.osNetworkId = subnet.getOpenstackId();
        this.openstackId = openstackId;
        this.macAddress = macAddress;
        this.ipAddresses = ipAddresses;
        this.subnet.addPort(this);
    }

    VMPort() {
    }

    public String getOsNetworkId() {
        return this.osNetworkId;
    }

    public String getOpenstackId() {
        return this.openstackId;
    }

    public List<String> getMacAddresses() {
        return Arrays.asList(this.macAddress);
    }

    public VM getVm() {
        return this.vm;
    }

    public void setOsNetworkId(String osNetworkId) {
        this.osNetworkId = osNetworkId;
    }

    public void setOpenstackId(String osPortId) {
        this.openstackId = osPortId;
    }

    void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public void setVm(VM vm) {
        if (null != vm){
            vm.addPort(this);
        }
        this.vm = vm;
    }

    public Network getNetwork() {
        return this.network;
    }

    public Subnet getSubnet() {
        return this.subnet;
    }

    public void setSubnet(Subnet subnet) {
        this.subnet = subnet;
    }

    @Override
    public String toString() {
        return "VMPort [osNetworkId=" + this.osNetworkId + ", openstackId=" + this.openstackId + ", macAddress=" + this.macAddress
                + ", ipAddresses=" + this.ipAddresses + ", vm=" + this.vm + ", network=" + this.network + ", subnet=" + this.subnet
                + ", dais=" + this.dais +  ", parentId=" + this.parentId+"]";
    }

    public String getElementId() {
        return getOpenstackId();
    }

    public Set<DistributedApplianceInstance> getDais() {
        return this.dais;
    }

    public void addDai(DistributedApplianceInstance dai) {
        this.dais.add(dai);
    }

    /**
     * Removes the dai references to this port and removes the dai from the list
     * of DAI's associated with this port
     *
     */
    public void removeDai(DistributedApplianceInstance dai) {
        dai.removeProtectedPort(this);
        this.dais.remove(dai);
    }

    /**
     * Removes all DAI references to this port and empties the DAI's associated with this port
     */
    public void removeAllDais() {
        for (DistributedApplianceInstance dai : this.dais) {
            dai.removeProtectedPort(this);
        }
        this.dais.clear();
    }

    public List<String> getPortIPs() {
        return this.ipAddresses;
    }

    public String getParentId() {
        return this.parentId;
    }

    public void setParentId(String parentId){
        this.parentId = parentId;
    }

}
