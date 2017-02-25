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
