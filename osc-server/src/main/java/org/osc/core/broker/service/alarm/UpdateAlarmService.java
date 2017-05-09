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
package org.osc.core.broker.service.alarm;

import javax.persistence.EntityManager;

import org.apache.commons.lang.StringUtils;
import org.osc.core.broker.model.entities.events.Alarm;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.UpdateAlarmServiceApi;
import org.osc.core.broker.service.dto.AlarmDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.AlarmEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.service.validator.AlarmDtoValidator;
import org.osc.core.broker.service.validator.BaseDtoValidator;
import org.osc.core.broker.util.ValidateUtil;
import org.osgi.service.component.annotations.Component;

@Component
public class UpdateAlarmService extends ServiceDispatcher<BaseRequest<AlarmDto>, BaseResponse>
        implements UpdateAlarmServiceApi {

    @Override
    public BaseResponse exec(BaseRequest<AlarmDto> request, EntityManager em) throws Exception {

        OSCEntityManager<Alarm> emgr = new OSCEntityManager<Alarm>(Alarm.class, em);

        // retrieve existing entry from db
        Alarm alarm = emgr.findByPrimaryKey(request.getDto().getId());

        // this validate function will throw exception if entry is not unique,
        // has empty fields, violates correct formatting or exceeds maximum allowed length
        validate(em, request.getDto(), alarm, emgr);

        AlarmEntityMgr.toEntity(alarm, request.getDto());
        emgr.update(alarm);
        BaseResponse response = new BaseResponse(alarm.getId());

        return response;
    }

    void validate(EntityManager em, AlarmDto dto, Alarm existingAlarm, OSCEntityManager<Alarm> emgr) throws Exception {

        BaseDtoValidator.checkForNullId(dto);

        // check for null/empty values
        AlarmDtoValidator.checkForNullFields(dto);
        //field length should not exceed current MAX_LEN = 155 chars
        AlarmDtoValidator.checkFieldLength(dto);

        //validating email address formatting
        if (!StringUtils.isBlank(dto.getReceipientEmail())) {
            ValidateUtil.checkForValidEmailAddress(dto.getReceipientEmail());
        }

        //validating the user entered regex syntax
        AlarmDtoValidator.checkRegexSyntax(dto);

        // entry must pre-exist in db
        if (existingAlarm == null) {

            throw new VmidcBrokerValidationException("Alarm entry with name " + dto.getName() + " is not found.");
        }
    }
}
