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
package org.osc.core.broker.service.persistence;

import org.osc.core.broker.model.entities.events.EmailSettings;
import org.osc.core.broker.service.email.EmailSettingsDto;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.encryption.EncryptionException;

public class EmailSettingsEntityMgr {

    public static EmailSettings createEntity(EmailSettingsDto dto) throws EncryptionException {
        EmailSettings EmailSettings = new EmailSettings();
        toEntity(EmailSettings, dto);
        return EmailSettings;
    }

    public static void toEntity(EmailSettings emailSettings, EmailSettingsDto dto) throws EncryptionException {

        // transform from dto to entity
        emailSettings.setMailServer(dto.getMailServer());
        emailSettings.setPort(Integer.valueOf(dto.getPort()));
        emailSettings.setEmailId(dto.getEmailId());
        emailSettings.setPassword(EncryptionUtil.encryptAESCTR(dto.getPassword()));
    }

    public static void fromEntity(EmailSettings emailSettings, EmailSettingsDto dto) throws EncryptionException {

        dto.setId(emailSettings.getId());
        dto.setMailServer(emailSettings.getMailServer());
        dto.setPort(emailSettings.getPort().toString());
        dto.setEmailId(emailSettings.getEmailId());
        dto.setPassword(EncryptionUtil.decryptAESCTR(emailSettings.getPassword()));
    }
}
