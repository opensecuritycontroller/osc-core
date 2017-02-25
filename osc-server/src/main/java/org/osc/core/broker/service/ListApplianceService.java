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
import org.hibernate.criterion.Order;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.service.dto.ApplianceDto;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.persistence.ApplianceEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.ListResponse;



public class ListApplianceService extends ServiceDispatcher<BaseRequest<BaseDto>, ListResponse<ApplianceDto>> {

    ListResponse<ApplianceDto> response = new ListResponse<ApplianceDto>();

    @Override
    public ListResponse<ApplianceDto> exec(BaseRequest<BaseDto> request, Session session) {
        // Initializing Entity Manager
        EntityManager<Appliance> emgr = new EntityManager<Appliance>(Appliance.class, session);
        // to do mapping
        List<ApplianceDto> dtoList = new ArrayList<ApplianceDto>();

        // mapping all the appliance objects to appliance dto objects
        for (Appliance a : emgr.listAll(new Order[] { Order.asc("model") })) {

            ApplianceDto dto = new ApplianceDto();

            ApplianceEntityMgr.fromEntity(a, dto);

            dtoList.add(dto);
        }

        this.response.setList(dtoList);
        return this.response;
    }

}
