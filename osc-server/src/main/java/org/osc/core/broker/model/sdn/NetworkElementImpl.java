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
package org.osc.core.broker.model.sdn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.osc.core.broker.model.entities.virtualization.k8s.PodPort;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.sdk.controller.element.NetworkElement;

public class NetworkElementImpl implements NetworkElement {

	private enum NetworkElementType {
		VM_PORT, POD_PORT, NETWORK_ELEMENT, GENERIC;
	}

    private final NetworkElementType type;
    private String elementId;
    private List<String> macAddresses = new ArrayList<>();
    private List<String> portIPs = new ArrayList<>();
    private String parentId;

    public NetworkElementImpl(String elementId) {
        this(elementId, null);
    }

    public NetworkElementImpl(String elementId, String parentId) {
        this.elementId = elementId;
        this.parentId = parentId;
        this.type = NetworkElementType.GENERIC;
    }

    public NetworkElementImpl(NetworkElement portGrp) {
        this.elementId = portGrp.getElementId();
        this.macAddresses = portGrp.getMacAddresses();
        this.portIPs = portGrp.getPortIPs();
        this.type = NetworkElementType.NETWORK_ELEMENT;
    }

    public NetworkElementImpl(VMPort vmPort) {
        this.elementId = vmPort.getOpenstackId();
        this.macAddresses = vmPort.getMacAddresses();
        this.portIPs = vmPort.getIpAddresses();
        this.parentId = vmPort.getParentId();
        this.type = NetworkElementType.VM_PORT;
    }

    public NetworkElementImpl(PodPort podPort) {
        this.elementId = podPort.getExternalId();
        this.macAddresses = Arrays.asList(podPort.getMacAddress());
        this.portIPs = podPort.getIpAddresses();
        this.parentId = podPort.getParentId();
        this.type = NetworkElementType.POD_PORT;
    }

    @Override
    public String getElementId() {
        return this.elementId;
    }

    @Override
    public List<String> getMacAddresses() {
        return Collections.unmodifiableList(this.macAddresses);
    }

    @Override
    public List<String> getPortIPs() {
        return Collections.unmodifiableList(this.portIPs);
    }

    @Override
    public String getParentId() {
        return this.parentId;
    }

    @Override
    public String toString() {
        return "NetworkElementImpl [type=" + this.type + ", elementId=" + this.elementId + ", macAddresses=" + this.macAddresses
                + ", portIPs=" + this.portIPs + ", parentId=" + this.parentId + "]";
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
}
