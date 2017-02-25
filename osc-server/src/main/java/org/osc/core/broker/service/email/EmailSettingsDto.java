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

import java.util.HashMap;
import java.util.Map;

import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.util.ValidateUtil;

public class EmailSettingsDto extends BaseDto {

    private String mailServer;
    private String port;
    private String emailId;
    private String password;

    public String getMailServer() {
        return mailServer;
    }

    public void setMailServer(String mailServer) {
        this.mailServer = mailServer;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static void checkForNullFields(EmailSettingsDto dto) throws Exception {
        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("mailServer", dto.getMailServer());
        map.put("port", dto.getPort());
        map.put("emailId", dto.getEmailId());
        ValidateUtil.checkForNullFields(map);
    }

    @Override
    public String toString() {
        return "EmailSettingsDto [mailServer=" + mailServer + ", port=" + port + ", emailId=" + emailId + ", password="
                + password + ", getId()=" + getId() + "]";
    }
}
