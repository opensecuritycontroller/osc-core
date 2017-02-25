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
