package org.osc.core.broker.model.entities.virtualization.openstack;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;

@SuppressWarnings("serial")
@Entity
@Table(name = "VM")
public class VM extends BaseEntity implements OsProtectionEntity {

    @Column(name = "region", nullable = false)
    private String region;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "vm_id", nullable = false, unique = true)
    private String openstackId;

    @Column(name = "host")
    private String host;

    @OneToMany(mappedBy = "vm", fetch = FetchType.LAZY)
    private Set<VMPort> ports = new HashSet<VMPort>();

    @OneToMany(mappedBy = "vm", fetch = FetchType.LAZY)
    private Set<SecurityGroupMember> securityGroupMembers = new HashSet<>();

    public VM(String region, String vmId, String name) {
        super();
        this.region = region;
        this.openstackId = vmId;
        this.name = name;
    }

    VM() {
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getOpenstackId() {
        return this.openstackId;
    }

    public String getHost() {
        return this.host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Set<VMPort> getPorts() {
        return this.ports;
    }

    void setPorts(Set<VMPort> ports) {
        this.ports = ports;
    }

    public void addPort(VMPort vmPort) {
        this.ports.add(vmPort);
    }

    public void removePort(VMPort vmPort) {
        this.ports.remove(vmPort);
    }

    @Override
    public String getRegion() {
        return this.region;
    }

    @Override
    public Set<SecurityGroupMember> getSecurityGroupMembers() {
        return this.securityGroupMembers;
    }

    public void addSecurityGroupMember(SecurityGroupMember member) {
        this.securityGroupMembers.add(member);
    }

    @Override
    public String toString() {
        return "VM [name=" + this.name + ", vmId=" + this.openstackId + ", host=" + this.host + "]";
    }

    @Override
    public SecurityGroupMemberType getType() {
        return SecurityGroupMemberType.VM;
    }

}
