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
package org.osc.core.broker.service.alarm;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.events.Alarm;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.AlarmEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.util.ValidateUtil;

public class AddAlarmService extends ServiceDispatcher<BaseRequest<AlarmDto>, BaseResponse> {

    @Override
    public BaseResponse exec(BaseRequest<AlarmDto> request, Session session) throws Exception {

        // Initializing Entity Manager
        EntityManager<Alarm> emgr = new EntityManager<Alarm>(Alarm.class, session);

        // this validate function will throw exception if entry is not unique,
        // has empty fields, violates correct formatting or exceeds maximum allowed length
        validate(session, request.getDto(), emgr);

        // creating new entry in the db using entity manager object
        Alarm alarm = AlarmEntityMgr.createEntity(request.getDto());
        alarm = emgr.create(alarm);

        BaseResponse response = new BaseResponse(alarm.getId());

        return response;
    }

    void validate(Session session, AlarmDto dto, EntityManager<Alarm> emgr) throws Exception {

        AlarmDto.checkForNullFields(dto);
        //validating email address formatting
        if (!StringUtils.isBlank(dto.getReceipientEmail())) {
            ValidateUtil.checkForValidEmailAddress(dto.getReceipientEmail());
        }
        //validating the user entered syntax
        AlarmDto.checkRegexSyntax(dto);

        AlarmDto.checkFieldLength(dto);

        // check for uniqueness of alarm name
        if (emgr.isExisting("name", dto.getName())) {

            throw new VmidcBrokerValidationException("Alarm name: " + dto.getName() + " already exists.");
        }

    }
}
