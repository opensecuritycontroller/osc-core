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

import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.api.ListDistributedApplianceInstanceServiceApi;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.dto.DistributedApplianceInstanceDto;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class ListDistributedApplianceInstanceService
        extends ServiceDispatcher<BaseRequest<BaseDto>, ListResponse<DistributedApplianceInstanceDto>>
        implements ListDistributedApplianceInstanceServiceApi {

    @Reference
    private ApiFactoryService apiFactoryService;

    ListResponse<DistributedApplianceInstanceDto> response = new ListResponse<DistributedApplianceInstanceDto>();

    @Override
    public ListResponse<DistributedApplianceInstanceDto> exec(BaseRequest<BaseDto> request, EntityManager em) throws Exception {
        OSCEntityManager<DistributedApplianceInstance> emgr = new OSCEntityManager<DistributedApplianceInstance>(
                DistributedApplianceInstance.class, em);
        List<DistributedApplianceInstanceDto> daiList = new ArrayList<DistributedApplianceInstanceDto>();
        for (DistributedApplianceInstance dai : emgr.listAll("name")) {
            Boolean providesDeviceStatus = this.apiFactoryService.providesDeviceStatus(dai.getVirtualSystem());
            DistributedApplianceInstanceDto dto = DistributedApplianceInstanceEntityMgr.fromEntity(dai, providesDeviceStatus);
            daiList.add(dto);
        }
        this.response.setList(daiList);
        return this.response;
    }
}
