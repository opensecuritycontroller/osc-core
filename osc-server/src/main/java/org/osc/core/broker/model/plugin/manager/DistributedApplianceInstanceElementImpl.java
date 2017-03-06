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

import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.sdk.manager.element.DistributedApplianceInstanceElement;
import org.osc.sdk.manager.element.VirtualSystemElement;

public class DistributedApplianceInstanceElementImpl implements DistributedApplianceInstanceElement {

    private final DistributedApplianceInstance entity;

    public DistributedApplianceInstanceElementImpl(DistributedApplianceInstance entity) {
        this.entity = entity;
    }

    @Override
    public Long getId() {
        return this.entity.getId();
    }

    @Override
    public String getName() {
        return this.entity.getName();
    }

    @Override
    public byte[] getApplianceConfig() {
        byte[] applianceConfig = this.entity.getApplianceConfig();
        return Arrays.copyOf(applianceConfig, applianceConfig.length);
    }

    @Override
    public VirtualSystemElement getVirtualSystem() {
        return new VirtualSystemElementImpl(this.entity.getVirtualSystem());
    }
}
