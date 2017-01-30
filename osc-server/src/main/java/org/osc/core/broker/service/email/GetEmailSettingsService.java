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
