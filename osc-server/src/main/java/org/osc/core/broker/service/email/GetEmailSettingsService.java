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
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.BaseDtoResponse;

public class GetEmailSettingsService extends ServiceDispatcher<Request, BaseDtoResponse<EmailSettingsDto>> {

    @Override
    public BaseDtoResponse<EmailSettingsDto> exec(Request request, Session session) throws Exception {
        EmailSettings emailSettings = (EmailSettings) session.get(EmailSettings.class, 1L);
        BaseDtoResponse<EmailSettingsDto> emailSettingsResponse = new BaseDtoResponse<EmailSettingsDto>();

        if (emailSettings != null) {
            EmailSettingsDto emailSettingsDto = new EmailSettingsDto();
            EmailSettingsEntityMgr.fromEntity(emailSettings, emailSettingsDto);

            emailSettingsResponse.setDto(emailSettingsDto);
        }

        return emailSettingsResponse;
    }
}
