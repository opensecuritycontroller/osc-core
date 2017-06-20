/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
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
package org.osc.core.broker.util;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.StringUtils;
import org.osc.core.broker.model.entities.events.Alert;
import org.osc.core.broker.service.dto.EmailSettingsDto;
import org.osc.core.broker.service.validator.EmailSettingsDtoValidator;

public class EmailUtil {

    /**
     *
     * This method will send an Email to receiver with Generated Alert Information
     *
     * @param smtpServer
     *            Mail server IP/FQDN
     * @param port
     *            SMTP port
     * @param sendFrom
     *            Sender's Email ID
     * @param password
     *            Email account Password
     * @param sendTo
     *            Receiver's Email address
     * @param alert
     *            Alert Object
     * @throws AddressException
     * @throws MessagingException
     */
    public static void sendEmail(String smtpServer, String port, final String sendFrom, final String password,
            String sendTo, Alert alert) throws AddressException, MessagingException {

        Session session = getSession(smtpServer, port, sendFrom, password);
        Message message = initializeMessage(session, sendFrom, sendTo, "OSC Failure Notification!");
        message.setText("An alert has been generated in your OSC environment.\n" + "Type: " + alert.getType() + "\n"
                + "Severity: " + alert.getSeverity() + "\n" + "Object: " + alert.getObjectName() + "\n" + "Message: "
                + alert.getMessage() + "\n"
                + "Please click on the following URL or login to your OSC for more details: " + createAlertUrl(alert));
        Transport.send(message);
    }

    /**
     *
     * This method will create a new Mail Session
     *
     * @param smtpServer
     *            Mail server IP/FQDN
     * @param port
     *            SMTP port
     * @param sendFrom
     *            Sender's Email ID
     * @param password
     *            Email account Password
     * @param sendTo
     *            Receiver's Email address
     * @return
     *         Mail Session Object with Properties configured
     */
    private static Session getSession(String smtpServer, String port, final String sendFrom, final String password) {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpServer);
        props.put("java.net.preferIPv4Stack", "true");
        props.put("mail.smtp.port", port);

        Session session = Session.getInstance(props, null);

        if (!StringUtils.isBlank(password)) {
            props.put("mail.smtp.socketFactory.port", port);
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.auth", "true");

            session = Session.getInstance(props, new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(sendFrom, password);
                }
            });
        }

        return session;
    }

    /**
     * @param dto
     *            Email Settings DTO which contains information to be validated
     * @throws Exception
     */
    public static void validateEmailSettings(EmailSettingsDto dto) throws Exception {
        EmailSettingsDtoValidator.checkForNullFields(dto);
        ValidateUtil.checkForValidPortNumber(dto.getPort());
        ValidateUtil.checkForValidEmailAddress(dto.getEmailId());
        ValidateUtil.checkForValidFqdn(dto.getMailServer());
    }

    /**
     * @param session
     *            Java Mail Session object
     * @param sendFrom
     *            Sender's Email Address
     * @param sendTo
     *            Receiver's Email Address
     * @param subject
     *            Subject of Email
     * @return
     *         a MimeMessage object
     * @throws MessagingException
     */
    private static Message initializeMessage(Session session, String sendFrom, String sendTo, String subject)
            throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(sendFrom));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(sendTo));
        message.setSentDate(new Date());
        message.setSubject(subject);
        return message;
    }

    /**
     *
     * This method will send a test Email to validate email settings configured by user
     *
     * @param smtpServer
     *            Mail server IP/FQDN
     * @param port
     *            SMTP port
     * @param sendFrom
     *            Sender's Email ID
     * @param password
     *            Email account Password
     * @param sendTo
     *            Receiver's Email address
     * @throws AddressException
     * @throws MessagingException
     */
    public static void sentTestEmail(String smtpServer, String port, final String sendFrom, final String password,
            String sendTo) throws AddressException, MessagingException {
        Session session = getSession(smtpServer, port, sendFrom, password);
        Message message = initializeMessage(session, sendFrom, sendTo, "Open Security Controller test email");
        message.setText("This email is to verify your Email settings provided on OSC server!");

        Transport.send(message);
    }

    /**
     * @param alert
     *            Alert Object
     * @return
     *         A Alert URL which can be used to see the Alert on ISC Server
     */
    private static String createAlertUrl(Alert alert) {
        return "https://" + ServerUtil.getServerIP() + "/#!Alerts/alertId=" + alert.getId();
    }
}
