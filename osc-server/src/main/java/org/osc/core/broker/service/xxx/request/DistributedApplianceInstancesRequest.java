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
package org.osc.core.broker.service.xxx.request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.service.dto.DistributedApplianceInstanceDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.util.ValidateUtil;

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

    public static void checkForNullFields(DistributedApplianceInstancesRequest request) throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> notNullFieldsMap = new HashMap<String, Object>();

        notNullFieldsMap.put("dtoIdList", request.getDtoIdList());

        for (Long id : request.getDtoIdList()) {
            if (id == null) {
                notNullFieldsMap.put("dtoId", id);
            }
        }
        ValidateUtil.checkForNullFields(notNullFieldsMap);

        if(request.getDtoIdList().isEmpty()) {
            throw new VmidcBrokerInvalidEntryException("dtoIdList should not be empty.");
        }
    }

    @Override
    public String toString() {
        return "DistributedApplianceInstancesRequest [dtoIdList=" + this.dtoIdList + "]";
    }
}
