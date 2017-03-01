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
package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.service.dto.DistributedApplianceInstanceDto;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListDistributedApplianceInstanceByVSService extends
        ServiceDispatcher<BaseIdRequest, ListResponse<DistributedApplianceInstanceDto>> {
    ListResponse<DistributedApplianceInstanceDto> response = new ListResponse<DistributedApplianceInstanceDto>();

    @Override
    public ListResponse<DistributedApplianceInstanceDto> exec(BaseIdRequest request, Session session) throws Exception {
        List<DistributedApplianceInstanceDto> dtoList = new ArrayList<DistributedApplianceInstanceDto>();
        for (DistributedApplianceInstance dai : DistributedApplianceInstanceEntityMgr.listByVsId(session,
                request.getId())) {
            DistributedApplianceInstanceDto dto = new DistributedApplianceInstanceDto(dai);
            dtoList.add(dto);
        }
        this.response.setList(dtoList);
        return this.response;
    }

}
