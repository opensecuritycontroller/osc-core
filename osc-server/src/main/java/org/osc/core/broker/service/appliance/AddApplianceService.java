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
package org.osc.core.broker.service.appliance;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.AddApplianceServiceApi;
import org.osc.core.broker.service.dto.ApplianceDto;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.service.validator.ApplianceDtoValidator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = {AddApplianceServiceApi.class})
public class AddApplianceService extends ServiceDispatcher<BaseRequest<ApplianceDto>, BaseResponse>
implements AddApplianceServiceApi {
    @Reference
    private ApiFactoryService apiFactoryService;

    @Reference
    private ApplianceDtoValidator applianceDtoValidator;

    @Override
    public BaseResponse exec(BaseRequest<ApplianceDto> request, EntityManager em) throws Exception {
        BaseResponse response = new BaseResponse();

        ApplianceDto applianceDto = request.getDto();

        this.applianceDtoValidator.validateForCreate(applianceDto);

        Appliance appliance = new Appliance();
        appliance.setManagerType(applianceDto.getManagerType());
        appliance.setModel(applianceDto.getModel());
        appliance.setManagerSoftwareVersion(applianceDto.getManagerVersion());

        OSCEntityManager<Appliance> applianceEntityManager = new OSCEntityManager<Appliance>(Appliance.class, em, this.txBroadcastUtil);

        appliance = applianceEntityManager.create(appliance);

        Long id = appliance.getId();

        response.setId(id);
        return response;
    }
}
