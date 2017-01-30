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
