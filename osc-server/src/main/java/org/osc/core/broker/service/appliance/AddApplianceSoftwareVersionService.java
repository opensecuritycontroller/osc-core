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
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.AddApplianceSoftwareVersionServiceApi;
import org.osc.core.broker.service.dto.ApplianceSoftwareVersionDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.ApplianceSoftwareVersionEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.service.validator.ApplianceSoftwareVersionDtoValidator;
import org.osc.core.broker.service.validator.DtoValidator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = {AddApplianceSoftwareVersionServiceApi.class})
public class AddApplianceSoftwareVersionService extends ServiceDispatcher<BaseRequest<ApplianceSoftwareVersionDto>, BaseResponse>
implements AddApplianceSoftwareVersionServiceApi {
    DtoValidator<ApplianceSoftwareVersionDto, ApplianceSoftwareVersion> validator;

    @Reference
    private ApiFactoryService apiFactoryService;

    @Reference
    private ApplianceSoftwareVersionDtoValidator validatorFactory;

    @Override
    public BaseResponse exec(BaseRequest<ApplianceSoftwareVersionDto> request, EntityManager em) throws Exception {
        BaseResponse response = new BaseResponse();

        ApplianceSoftwareVersionDto asvDto = request.getDto();

        if (this.validator == null) {
            this.validator = this.validatorFactory.create(em);
        }

        this.validator.validateForCreate(asvDto);

        OSCEntityManager<Appliance> applianceEntityManager = new OSCEntityManager<Appliance>(Appliance.class, em, this.txBroadcastUtil);
        Appliance appliance = applianceEntityManager.findByPrimaryKey(asvDto.getParentId());

        if (appliance == null) {
            throw new VmidcBrokerValidationException("An appliance was not found for the id: " + asvDto.getParentId());
        }

        OSCEntityManager<ApplianceSoftwareVersion> emgr = new OSCEntityManager<ApplianceSoftwareVersion>(
                ApplianceSoftwareVersion.class, em, this.txBroadcastUtil);

        ApplianceSoftwareVersion applianceSoftwareVersion = ApplianceSoftwareVersionEntityMgr.createEntity(em, asvDto, appliance);

        applianceSoftwareVersion = emgr.create(applianceSoftwareVersion);

        response.setId(applianceSoftwareVersion.getId());
        return response;
    }
}
