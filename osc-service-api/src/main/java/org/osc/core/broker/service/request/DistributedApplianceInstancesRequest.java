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
package org.osc.core.broker.service.request;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.service.dto.DistributedApplianceInstanceDto;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class DistributedApplianceInstancesRequest implements Request {

    public List<Long> dtoIdList = new ArrayList<Long>();

    DistributedApplianceInstancesRequest() {

    }

    public DistributedApplianceInstancesRequest(List<DistributedApplianceInstanceDto> dtoList) {
        if (dtoList != null) {
            for (DistributedApplianceInstanceDto dai : dtoList) {
                this.dtoIdList.add(dai.getId());
            }
        }
    }


    public List<Long> getDtoIdList() {
        return this.dtoIdList;
    }

    @Override
    public String toString() {
        return "DistributedApplianceInstancesRequest [dtoIdList=" + this.dtoIdList + "]";
    }
}
