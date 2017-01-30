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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.ForeignKey;
import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.sdk.controller.element.NetworkElement;

@SuppressWarnings("serial")
@Entity
@Table(name = "VM_PORT")
public class VMPort extends BaseEntity implements NetworkElement {

    @Column(name = "os_network_id", nullable = false)
    private String osNetworkId;

    @Column(name = "os_port_id", nullable = false, unique = true)
    private String openstackId;

    @Column(name = "mac_address", nullable = false, unique = true)
    private String macAddress;

    @ElementCollection(fetch = FetchType.LAZY)
    @Column(name = "ip_address")
    @ForeignKey(name = "FK_VM_PORT_IP_ADDRESS")
    @CollectionTable(name = "VM_PORT_IP_ADDRESS", joinColumns = @JoinColumn(name = "vm_port_fk"))
    private List<String> ipAddresses = new ArrayList<String>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vm_fk")
    @ForeignKey(name = "FK_VMP_VM")
    private VM vm;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "network_fk")
    @ForeignKey(name = "FK_VMP_NETWORK")
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
    @JoinColumn(name = "subnet_fk")
    @ForeignKey(name = "FK_VMP_SUBNET")
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

    @Override
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

    @Override
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

    @Override
    public List<String> getPortIPs() {
        return this.ipAddresses;
    }

    @Override
    public String getParentId() {
        return this.parentId;
    }

    public void setParentId(String parentId){
        this.parentId = parentId;
    }

}
