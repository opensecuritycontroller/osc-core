package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osc.sdk.controller.element.NetworkElement;

public class PortGroup implements NetworkElement {

    private String portGroupId;
    private List<String> macAddresses = new ArrayList<String>();
    private List<String> portGroupIPs = new ArrayList<String>();
    private String parentId;

    public PortGroup() {
    }

    public PortGroup(NetworkElement portGrp) {
        this.portGroupId = portGrp.getElementId();
        this.macAddresses = portGrp.getMacAddresses();
        this.portGroupIPs = portGrp.getPortIPs();
    }

    public PortGroup(String portGrpId, String macAddress) {
        this.portGroupId = portGrpId;
        this.macAddresses.add(macAddress);
    }

    @Override
    public String getElementId() {
        return this.portGroupId;
    }

    @Override
    public List<String> getMacAddresses() {
        return Collections.unmodifiableList(this.macAddresses);
    }

    public void addMacAddress(String macAddress){
        this.macAddresses.add(macAddress);
    }

    @Override
    public String toString() {
        return "PortGroup [portGroupId=" + this.portGroupId + ", macAddresses=" + this.macAddresses
                + ", portGroupIps=" + this.portGroupIPs + "]";
    }

    @Override
    public List<String> getPortIPs() {
        return Collections.unmodifiableList(this.portGroupIPs);
    }

    public void addPortIP(String portIP){
        this.portGroupIPs.add(portIP);
    }

    public void setPortGroupId(String portGrpId) {
        this.portGroupId = portGrpId;
    }

    @Override
    public String getParentId() {
        return this.parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
}
