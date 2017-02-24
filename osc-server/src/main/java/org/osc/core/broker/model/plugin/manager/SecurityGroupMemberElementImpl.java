package org.osc.core.broker.model.plugin.manager;

import java.util.ArrayList;
import java.util.List;

import org.osc.sdk.manager.element.SecurityGroupMemberElement;

public class SecurityGroupMemberElementImpl implements SecurityGroupMemberElement {

    private String id;
    private String name;
    private List<String> ipAddresses = new ArrayList<String>();
    private List<String> macAddresses = new ArrayList<String>();

    public SecurityGroupMemberElementImpl(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public void addIpAddress(String ipAddress) {
        this.ipAddresses.add(ipAddress);
    }

    public void addIpAddress(List<String> ipAddressList) {
        this.ipAddresses.addAll(ipAddressList);
    }

    public void addMacAddress(String macAddress) {
        this.macAddresses.add(macAddress);
    }

    public void addMacAddresses(List<String> macAddressList) {
        this.macAddresses.addAll(macAddressList);
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public List<String> getIpAddresses() {
        return this.ipAddresses;
    }

    @Override
    public List<String> getMacAddresses() {
        return this.macAddresses;
    }

}
