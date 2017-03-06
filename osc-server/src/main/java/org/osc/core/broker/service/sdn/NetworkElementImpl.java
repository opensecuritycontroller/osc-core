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
package org.osc.core.broker.service.sdn;

import java.util.List;

import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.sdk.controller.element.NetworkElement;

public class NetworkElementImpl implements NetworkElement {

    private final VMPort vmPort;

    public NetworkElementImpl(VMPort vmPort) {
        this.vmPort = vmPort;
    }

    @Override
    public String getElementId() {
        return this.vmPort.getElementId();
    }

    @Override
    public List<String> getMacAddresses() {
        return this.vmPort.getMacAddresses();
    }

    @Override
    public List<String> getPortIPs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getParentId() {
        // TODO Auto-generated method stub
        return null;
    }

}
