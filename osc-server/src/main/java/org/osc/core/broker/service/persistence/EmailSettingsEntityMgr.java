package org.osc.core.broker.service.persistence;

import org.osc.core.broker.model.entities.events.EmailSettings;
import org.osc.core.broker.service.email.EmailSettingsDto;
import org.osc.core.util.EncryptionUtil;

public class EmailSettingsEntityMgr {

    public static EmailSettings createEntity(EmailSettingsDto dto) {
        EmailSettings EmailSettings = new EmailSettings();
        toEntity(EmailSettings, dto);
        return EmailSettings;
    }

    public static void toEntity(EmailSettings emailSettings, EmailSettingsDto dto) {

        // transform from dto to entity
        emailSettings.setMailServer(dto.getMailServer());
        emailSettings.setPort(Integer.valueOf(dto.getPort()));
        emailSettings.setEmailId(dto.getEmailId());
        emailSettings.setPassword(EncryptionUtil.encrypt(dto.getPassword()));
    }

    public static void fromEntity(EmailSettings emailSettings, EmailSettingsDto dto) {

        dto.setId(emailSettings.getId());
        dto.setMailServer(emailSettings.getMailServer());
        dto.setPort(emailSettings.getPort().toString());
        dto.setEmailId(emailSettings.getEmailId());
        dto.setPassword(EncryptionUtil.decrypt(emailSettings.getPassword()));
    }
}
