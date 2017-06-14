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

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.service.api.ListDistributedApplianceServiceApi;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.EncryptionException;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.persistence.DistributedApplianceEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class ListDistributedApplianceService
        extends ServiceDispatcher<BaseRequest<BaseDto>, ListResponse<DistributedApplianceDto>>
        implements ListDistributedApplianceServiceApi {

    @Reference
    private EncryptionApi encrypter;


    @Override
    public ListResponse<DistributedApplianceDto> exec(BaseRequest<BaseDto> request, EntityManager em) throws EncryptionException {
        ListResponse<DistributedApplianceDto> response = new ListResponse<DistributedApplianceDto>();
        // Initializing Entity Manager
        OSCEntityManager<DistributedAppliance> emgr = new OSCEntityManager<DistributedAppliance>(DistributedAppliance.class,
                em, this.txBroadcastUtil);
        // to do mapping
        List<DistributedApplianceDto> dtoList = new ArrayList<DistributedApplianceDto>();

        // mapping all the da objects to da dto objects
        for (DistributedAppliance da : emgr.listAll("name")) {

            DistributedApplianceDto dto = new DistributedApplianceDto();

            DistributedApplianceEntityMgr.fromEntity(da, dto, this.encrypter);
            if(request.isApi()) {
                DistributedApplianceDto.sanitizeDistributedAppliance(dto);
            }
            dtoList.add(dto);
        }

        response.setList(dtoList);
        return response;
    }

}
