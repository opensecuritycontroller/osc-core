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
package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.persistence.DistributedApplianceEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.util.encryption.EncryptionException;


public class ListDistributedApplianceService extends
        ServiceDispatcher<BaseRequest<BaseDto>, ListResponse<DistributedApplianceDto>> {

    ListResponse<DistributedApplianceDto> response = new ListResponse<DistributedApplianceDto>();

    @Override
    public ListResponse<DistributedApplianceDto> exec(BaseRequest<BaseDto> request, Session session) throws EncryptionException {
        // Initializing Entity Manager
        EntityManager<DistributedAppliance> emgr = new EntityManager<DistributedAppliance>(DistributedAppliance.class,
                session);
        // to do mapping
        List<DistributedApplianceDto> dtoList = new ArrayList<DistributedApplianceDto>();

        // mapping all the da objects to da dto objects
        for (DistributedAppliance da : emgr.listAll(new Order[] { Order.asc("name") })) {

            DistributedApplianceDto dto = new DistributedApplianceDto();

            DistributedApplianceEntityMgr.fromEntity(da, dto);
            if(request.isApi()) {
                DistributedApplianceDto.sanitizeDistributedAppliance(dto);
            }
            dtoList.add(dto);
        }

        this.response.setList(dtoList);
        return this.response;
    }

}
