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
package org.osc.core.broker.service.validator;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.commons.lang.NotImplementedException;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.service.dto.ApplianceDto;
import org.osc.core.broker.service.persistence.ApplianceEntityMgr;
import org.osc.core.broker.util.ValidateUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = ApplianceDtoValidator.class)
public class ApplianceDtoValidator implements DtoValidator<ApplianceDto, Appliance> {

    @Reference
    private EntityManager em;

    @Override
    public void validateForCreate(ApplianceDto dto) throws Exception {
        if (dto == null) {
            throw new IllegalArgumentException("Cannot create Appliance with null DTO!");
        }

        Appliance existingAppliance = ApplianceEntityMgr.findByModel(this.em, dto.getModel());

        if (existingAppliance != null) {
            throw new IllegalArgumentException("Appliance already exists for model: " + dto.getModel());
        }

        checkForNullFields(dto);
    }

    @Override
    public Appliance validateForUpdate(ApplianceDto dto) throws Exception {
        throw new NotImplementedException("Method validate for update not implemented yet");
    }

    public static void checkForNullFields(ApplianceDto dto) throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("Appliance Model", dto.getModel());
        map.put("Appliance Manager Type", dto.getManagerType());
        map.put("Appliance Manager Version", dto.getManagerVersion());

        ValidateUtil.checkForNullFields(map);
    }

    public static void checkFieldLength(ApplianceDto dto) throws Exception {

        Map<String, String> map = new HashMap<String, String>();

        map.put("Appliance Model", dto.getModel());
        map.put("Appliance Manager Version", dto.getManagerVersion());

        ValidateUtil.validateFieldLength(map, ValidateUtil.DEFAULT_MAX_LEN);
    }
}