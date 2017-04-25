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
package org.osc.core.broker.view.alarm;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.events.AlarmAction;
import org.osc.core.broker.model.entities.events.EventType;
import org.osc.core.broker.model.entities.events.Severity;
import org.osc.core.broker.service.dto.AlarmDto;
import org.osc.core.broker.service.email.EmailSettingsDto;
import org.osc.core.broker.service.email.GetEmailSettingsService;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.BaseDtoResponse;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.CRUDBaseWindow;
import org.osc.core.broker.window.button.OkCancelButtonModel;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.validator.EmailValidator;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;

public abstract class BaseAlarmWindow extends CRUDBaseWindow<OkCancelButtonModel> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(BaseAlarmWindow.class);

    private ValueChangeListener actionChangedListener;

    // current view reference
    protected AlarmView alarmView = null;

    protected CheckBox enabled = null;
    protected TextField alarmName = null;
    protected ComboBox eventType = null;
    protected TextField regexMatch = null;
    protected ComboBox severity = null;
    protected ComboBox alarmAction = null;
    protected TextField email = null;

    public BaseAlarmWindow() {
        super();
        initListeners();
    }

    @Override
    public boolean validateForm() {
        try {
            this.alarmName.validate();
            this.eventType.validate();
            this.severity.validate();
            this.alarmAction.validate();
            if (this.alarmAction.getValue().equals(AlarmAction.EMAIL)) {
                this.email.validate();
                GetEmailSettingsService emailService = new GetEmailSettingsService();

                BaseDtoResponse<EmailSettingsDto> emailSettingsResponse = emailService.dispatch(new Request() {
                });
                if (emailSettingsResponse.getDto() == null) {
                    throw new VmidcException("Email settings have not been configured! ");
                }
            }
            return true;
        } catch (Exception e) {
            log.debug("Validation Error");
            ViewUtil.iscNotification(e.getMessage() + ".", Notification.Type.ERROR_MESSAGE);
        }
        return false;
    }

    @Override
    public void populateForm() {
        this.enabled = new CheckBox("Enabled");
        this.alarmName = new TextField("Alarm Name");
        this.alarmName.setImmediate(true);

        this.eventType = new ComboBox("Event Type");
        this.eventType.setTextInputAllowed(false);
        this.eventType.setNullSelectionAllowed(false);
        this.eventType.addItem(EventType.JOB_FAILURE);
        this.eventType.addItem(EventType.SYSTEM_FAILURE);
        this.eventType.addItem(EventType.DAI_FAILURE);
        this.eventType.select(EventType.JOB_FAILURE);

        this.regexMatch = new TextField("Regex Match");
        this.regexMatch.setValue(".*");
        this.regexMatch.setImmediate(true);
        this.regexMatch.setVisible(true);

        this.severity = new ComboBox("Severity");
        this.severity.setTextInputAllowed(false);
        this.severity.setNullSelectionAllowed(false);
        this.severity.addItem(Severity.HIGH);
        this.severity.addItem(Severity.MEDIUM);
        this.severity.addItem(Severity.LOW);
        this.severity.select(Severity.LOW);

        this.alarmAction = new ComboBox("Alarm Action");
        this.alarmAction.setTextInputAllowed(false);
        this.alarmAction.setNullSelectionAllowed(false);
        this.alarmAction.addItem(AlarmAction.NONE);
        this.alarmAction.addItem(AlarmAction.EMAIL);
        this.alarmAction.select(AlarmAction.NONE);
        this.alarmAction.addValueChangeListener(this.actionChangedListener);

        this.email = new TextField("Send email to");
        this.email.addValidator(new EmailValidator("Please enter a valid email address"));
        this.email.setImmediate(true);
        this.email.setVisible(false);

        // adding not null constraint
        this.alarmName.setRequired(true);
        this.alarmName.setRequiredError("Alarm Name cannot be empty");
        this.eventType.setRequired(true);
        this.eventType.setRequiredError("Event type cannot be empty");
        this.severity.setRequired(true);
        this.alarmAction.setRequired(true);
        this.alarmAction.setRequiredError("Alarm Action cannot be empty");
        this.email.setRequired(true);
        this.email.setRequiredError("Email address cannot be empty");

        this.form.setMargin(true);
        this.form.setSizeUndefined();

        this.form.addComponent(this.enabled);
        this.form.addComponent(this.alarmName);
        this.form.addComponent(this.eventType);
        this.form.addComponent(this.regexMatch);
        this.form.addComponent(this.severity);
        this.form.addComponent(this.alarmAction);
        this.form.addComponent(this.email);
        this.alarmName.focus();
    }

    @SuppressWarnings("serial")
    private void initListeners() {

        this.actionChangedListener = new ValueChangeListener() {

            @Override
            public void valueChange(ValueChangeEvent event) {
                if (BaseAlarmWindow.this.alarmAction.getValue().equals(AlarmAction.EMAIL)) {
                    BaseAlarmWindow.this.email.setVisible(true);
                } else {
                    BaseAlarmWindow.this.email.setVisible(false);
                }
            }
        };
    }

    protected BaseRequest<AlarmDto> createRequest() throws Exception {
        BaseRequest<AlarmDto> request = new BaseRequest<AlarmDto>();
        request.setDto(new AlarmDto());

        request.getDto().setEnabledAlarm(this.enabled.getValue());
        request.getDto().setName(this.alarmName.getValue().trim());
        request.getDto().setEventType(EventType.fromText(this.eventType.getValue().toString()));
        request.getDto().setRegexMatch(this.regexMatch.getValue().trim());
        request.getDto().setSeverity(Severity.fromText(this.severity.getValue().toString()));
        request.getDto().setAlarmAction(AlarmAction.fromText(this.alarmAction.getValue().toString()));
        request.getDto().setReceipientEmail(this.email.getValue().trim());

        return request;
    }

}
