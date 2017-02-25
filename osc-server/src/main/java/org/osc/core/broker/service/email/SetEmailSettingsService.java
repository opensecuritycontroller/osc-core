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
package org.osc.core.broker.service.email;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.events.EmailSettings;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.persistence.EmailSettingsEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;
import org.osc.core.broker.util.EmailUtil;

public class SetEmailSettingsService extends ServiceDispatcher<BaseRequest<EmailSettingsDto>, EmptySuccessResponse> {

    @Override
    public EmptySuccessResponse exec(BaseRequest<EmailSettingsDto> request, Session session) throws Exception {
        EntityManager<EmailSettings> emgr = new EntityManager<EmailSettings>(EmailSettings.class, session);

        validate(request);

        EmailSettings emailSettings = new EmailSettings();

        if (emgr.listAll().size() > 0) {
            emailSettings = emgr.listAll().get(0);
            EmailSettingsEntityMgr.toEntity(emailSettings, request.getDto());
            emgr.update(emailSettings);
        } else {
            emailSettings = EmailSettingsEntityMgr.createEntity(request.getDto());
            emgr.create(emailSettings);
        }

        return new EmptySuccessResponse();
    }

    void validate(BaseRequest<EmailSettingsDto> req) throws Exception {
        EmailUtil.validateEmailSettings(req.getDto());
    }
}
