package org.osc.core.broker.model.entities.events;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.osc.core.broker.model.entities.BaseEntity;

@SuppressWarnings("serial")
@Entity
@Table(name = "EMAIL_SETTINGS")
public class EmailSettings extends BaseEntity {

    public EmailSettings() {
        super();
    }

    @Column(name = "mail_server", nullable = false)
    private String mailServer;

    @Column(name = "port", nullable = false)
    private Integer port;

    @Column(name = "email_id", nullable = false)
    private String emailId;

    @Column(name = "password")
    private String password;

    public String getMailServer() {
        return mailServer;
    }

    public void setMailServer(String mailServer) {
        this.mailServer = mailServer;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
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

    @Override
    public String toString() {
        return "EmailSettings [mailServer=" + mailServer + ", port=" + port + ", emailId=" + emailId + ", password="
                + password + ", getId()=" + getId() + "]";
    }
}
