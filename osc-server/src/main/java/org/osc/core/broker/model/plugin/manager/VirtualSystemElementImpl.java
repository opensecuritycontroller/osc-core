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

import java.util.Arrays;

import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.sdk.manager.element.ApplianceSoftwareVersionElement;
import org.osc.sdk.manager.element.DistributedApplianceElement;
import org.osc.sdk.manager.element.DomainElement;
import org.osc.sdk.manager.element.VirtualSystemElement;
import org.osc.sdk.manager.element.VirtualizationConnectorElement;

public class VirtualSystemElementImpl implements VirtualSystemElement {

    private final VirtualSystem virtualSystem;

    public VirtualSystemElementImpl(VirtualSystem virtualSystem) {
        this.virtualSystem = virtualSystem;
    }

    @Override
    public Long getId() {
        return this.virtualSystem.getId();
    }

    @Override
    public String getName() {
        return this.virtualSystem.getName();
    }

    @Override
    public String getMgrId() {
        return this.virtualSystem.getMgrId();
    }

    @Override
    public DistributedApplianceElement getDistributedAppliance() {
        return new DistributedApplianceElementImpl(this.virtualSystem.getDistributedAppliance());
    }

    @Override
    public VirtualizationConnectorElement getVirtualizationConnector() {
        return new VirtualizationConnectorElementImpl(
                this.virtualSystem.getVirtualizationConnector());
    }

    @Override
    public ApplianceSoftwareVersionElement getApplianceSoftwareVersion() {
        return new ApplianceSoftwareVersionElementImpl(
                this.virtualSystem.getApplianceSoftwareVersion());
    }

    @Override
    public DomainElement getDomain() {
        return new DomainElementImpl(this.virtualSystem.getDomain());
    }

    @Override
    public byte[] getKeyStore() {
        byte[] keyStore = this.virtualSystem.getKeyStore();
        return Arrays.copyOf(keyStore, keyStore.length);
    }

}
