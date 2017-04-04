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

import org.osc.sdk.controller.FailurePolicyType;
import org.osc.sdk.controller.TagEncapsulationType;
import org.osc.sdk.controller.element.InspectionHookElement;
import org.osc.sdk.controller.element.InspectionPortElement;
import org.osc.sdk.controller.element.NetworkElement;

public class InspectionHookElementImpl implements InspectionHookElement {
    String id;
    Long tag;
    Long order;
    TagEncapsulationType encapsulationType;
    FailurePolicyType failurePolicyType;
    NetworkElement inspectedPort;
    InspectionPortElement inspectionPort;

    public InspectionHookElementImpl(
            String id,
            NetworkElement inspectedPort,
            InspectionPortElement inspectionPort,
            Long tag,
            TagEncapsulationType encapsulationType,
            Long order,
            FailurePolicyType failurePolicyType) {
        this.id = id;
        this.tag = tag;
        this.order = order;
        this.encapsulationType = encapsulationType;
        this.failurePolicyType = failurePolicyType;
        this.inspectedPort = inspectedPort;
        this.inspectionPort = inspectionPort;
    }

    @Override
    public String getHookId() {
        return this.id;
    }

    @Override
    public Long getTag() {
        return this.tag;
    }

    @Override
    public Long getOrder() {
        return this.order;
    }

    @Override
    public TagEncapsulationType getEncType() {
        return this.encapsulationType;
    }

    @Override
    public FailurePolicyType getFailurePolicyType() {
        return this.failurePolicyType;
    }

    @Override
    public NetworkElement getInspectedPort() {
        return this.inspectedPort;
    }

    @Override
    public InspectionPortElement getInspectionPort() {
        return this.inspectionPort;
    }

}
