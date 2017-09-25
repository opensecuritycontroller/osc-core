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
package org.osc.core.broker.model.plugin.sdncontroller;

import java.util.Arrays;
import java.util.List;

import org.osc.core.broker.model.entities.virtualization.k8s.PodPort;
import org.osc.sdk.controller.element.NetworkElement;

public class PodNetworkElementImpl implements NetworkElement {

    private final PodPort podPort;

    public PodNetworkElementImpl(PodPort vmPort) {
        this.podPort = vmPort;
    }

    @Override
    public String getElementId() {
        return this.podPort.getExternalId();
    }

    @Override
    public List<String> getMacAddresses() {
        return Arrays.asList(this.podPort.getMacAddress());
    }

    @Override
    public List<String> getPortIPs() {
        return this.podPort.getIpAddresses();
    }

    @Override
    public String getParentId() {
        return this.podPort.getParentId();
    }

    public void setParentId(String parentId) {
        this.podPort.setParentId(parentId);
    }

    @Override
    public String toString() {
        return "NetworkElementImpl [vmPort=" + this.podPort + "]";
    }
}
