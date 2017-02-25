/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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

public class HostDto extends BaseDto {

    private String openstackId;
    private String name;

    public HostDto() {
    }

    public HostDto(String openstackId, String name) {
        setOpenstackId(openstackId);
        this.name = name;
    }

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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.openstackId == null) ? 0 : this.openstackId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        HostDto other = (HostDto) obj;
        if (this.openstackId == null) {
            if (other.openstackId != null) {
                return false;
            }
        } else if (!this.openstackId.equals(other.openstackId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "HostDto [openstackId=" + this.openstackId + ", name=" + this.name + "]";
    }

}
