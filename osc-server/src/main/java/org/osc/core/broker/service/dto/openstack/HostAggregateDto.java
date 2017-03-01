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
package org.osc.core.broker.service.dto.openstack;

import org.osc.core.broker.service.dto.BaseDto;

public class HostAggregateDto extends BaseDto {

    private String name;
    private String openstackId;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOpenstackId() {
        return this.openstackId;
    }

    public void setOpenstackId(String openstackId) {
        this.openstackId = openstackId;
    }

    @Override
    public String toString() {
        return "HostAggregateDto [name=" + name + ", openstackId=" + openstackId + "]";
    }


}