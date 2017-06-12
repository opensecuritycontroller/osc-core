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
package org.osc.core.broker.window.update;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.GetEmailSettingsServiceApi;
import org.osc.core.broker.service.api.SetEmailSettingsServiceApi;
import org.osc.core.broker.service.dto.EmailSettingsDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.BaseDtoResponse;
import org.osc.core.broker.view.maintenance.EmailLayout;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.CRUDBaseValidateWindow;
import org.osc.core.broker.window.button.OkCancelValidateButtonModel;

import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;

public class SetEmailSettingsWindow extends CRUDBaseValidateWindow {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(SetEmailSettingsWindow.class);

    final String CAPTION = "Set Email Settings";

    private TextField smtp = null;
    private TextField port = null;
    private TextField emailId = null;
    private PasswordField password = null;
    private EmailLayout emailLayout = null;

    private final GetEmailSettingsServiceApi getEmailSettingsService;

    private final SetEmailSettingsServiceApi setEmailSettingsService;

    public SetEmailSettingsWindow(EmailLayout emailLayout, GetEmailSettingsServiceApi getEmailSettingsService,
            SetEmailSettingsServiceApi setEmailSettingsService) throws Exception {
        super(new OkCancelValidateButtonModel());
        this.emailLayout = emailLayout;
        this.getEmailSettingsService = getEmailSettingsService;
        this.setEmailSettingsService = setEmailSettingsService;
        createWindow(this.CAPTION);

    }

    @Override
    public void populateForm() {
        this.smtp = new TextField("Outgoing Mail Server (SMTP)");
        this.smtp.setImmediate(true);
        this.port = new TextField("Port");
        this.port.setImmediate(true);
        this.emailId = new TextField("Email Id");
        this.emailId.setImmediate(true);
        this.password = new PasswordField("Password");
        this.password.setImmediate(true);

        // filling form with existing data
        BaseDtoResponse<EmailSettingsDto> res = new BaseDtoResponse<EmailSettingsDto>();
        try {
            res = this.getEmailSettingsService.dispatch(new Request() {
            });

            if (res.getDto() != null) {
                this.smtp.setValue(res.getDto().getMailServer());
                this.port.setValue(res.getDto().getPort());
                this.emailId.setValue(res.getDto().getEmailId());
                this.password.setValue(res.getDto().getPassword());
            }

            // adding not null constraint
            this.smtp.setRequired(true);
            this.smtp.setRequiredError("SMTP server cannot be empty");
            this.port.setRequired(true);
            this.port.setRequiredError("SMTP port cannot be empty");
            this.emailId.setRequired(true);
            this.emailId.setRequiredError("Email Id cannot be empty");

            this.form.addComponent(this.smtp);
            this.smtp.focus();
            this.form.addComponent(this.port);
            this.form.addComponent(this.emailId);
            this.form.addComponent(this.password);

        } catch (Exception ex) {
            log.error("Failed to get email settings", ex);
            ViewUtil.iscNotification("Failed to get email settings (" + ex.getMessage() + ")",
                    Notification.Type.ERROR_MESSAGE);
        }

    }

    @Override
    public boolean validateForm() {
        try {
            this.smtp.validate();
            this.port.validate();
            this.emailId.validate();
            return true;
        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage() + ".", Notification.Type.ERROR_MESSAGE);
        }

        return false;
    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                BaseRequest<EmailSettingsDto> request = new BaseRequest<EmailSettingsDto>();
                request.setDto(getDto());
                this.setEmailSettingsService.dispatch(request);
                this.emailLayout.populateEmailtable();
                close();
            }
        } catch (Exception e) {
            log.error("Failed to update the email settings", e);
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }

    }

    private EmailSettingsDto getDto() {
        EmailSettingsDto dto = new EmailSettingsDto();
        dto.setMailServer(this.smtp.getValue().trim());
        dto.setPort(this.port.getValue().trim());
        dto.setEmailId(this.emailId.getValue().trim());
        dto.setPassword(this.password.getValue().trim());
        return dto;
    }

    @Override
    public void validateSettings() {
        try {
            if (validateForm()) {

                // Validate Email settings by sending an Email From and To the same email ID provided
                EmailSettingsDto dto = getDto();
                this.setEmailSettingsService.validateEmailSettings(new BaseRequest<>(dto));

                // If every things is correct attempt to send an email..
                this.setEmailSettingsService.sentTestEmail(dto.getMailServer(), dto.getPort(), dto.getEmailId(), dto.getPassword(),
                        dto.getEmailId());
                ViewUtil.iscNotification("Info: ",
                        "Email validation successful. You will be receiving an email shortly.", Type.HUMANIZED_MESSAGE);
            }
        } catch (Exception ex) {
            ViewUtil.iscNotification(
                    "Email settings are incorrect. Please re-try again with correct information " + ex.getMessage(),
                    Type.ERROR_MESSAGE);

            log.error("Failed to send email to the interested user(s): ", ex);

        }

    }
}