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
package org.osc.core.broker.service.manager;

import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.sdk.manager.element.ApplianceElement;
import org.osc.sdk.manager.element.ApplianceManagerConnectorElement;
import org.osc.sdk.manager.element.DistributedApplianceElement;

public class DistributedApplianceElementImpl implements DistributedApplianceElement {

    private final DistributedAppliance distributedAppliance;

    public DistributedApplianceElementImpl(DistributedAppliance distributedAppliance) {
        this.distributedAppliance = distributedAppliance;
    }

    @Override
    public ApplianceManagerConnectorElement getApplianceManagerConnector() {
        return new ApplianceManagerConnectorElementImpl(
                this.distributedAppliance.getApplianceManagerConnector());
    }

    @Override
    public ApplianceElement getAppliance() {
        return new ApplianceElementImpl(this.distributedAppliance.getAppliance());
    }

    @Override
    public String getApplianceVersion() {
        return this.distributedAppliance.getApplianceVersion();
    }

    @Override
    public String getMgrSecretKey() {
        return this.distributedAppliance.getMgrSecretKey();
    }

}
