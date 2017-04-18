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
import org.osc.core.broker.service.api.GetEmailSettingsServiceApi;
import org.osc.core.broker.service.api.UpdateAlarmServiceApi;
import org.osc.core.broker.service.dto.AlarmDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.view.util.ViewUtil;

import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.Notification;

public class UpdateAlarmWindow extends BaseAlarmWindow {

    private static final long serialVersionUID = 1L;
    final String CAPTION = "Edit Alarm";

    private static final Logger log = Logger.getLogger(UpdateAlarmWindow.class);

    // current view reference
    private AlarmView alarmView = null;

    private BeanItem<AlarmDto> currentAlarm = null;

    private UpdateAlarmServiceApi updateAlarmService;

    public UpdateAlarmWindow(AlarmView alarmView, UpdateAlarmServiceApi updateAlarmService,
            GetEmailSettingsServiceApi getEmailSettingsService) throws Exception {

        super(getEmailSettingsService);
        this.alarmView = alarmView;
        this.updateAlarmService = updateAlarmService;
        this.currentAlarm = alarmView.getParentItem();
        createWindow(this.CAPTION);
    }

    @Override
    public void populateForm() {

        super.populateForm();

        // filling fields with existing information
        this.enabled.setValue((Boolean) this.currentAlarm.getItemProperty("enabledAlarm").getValue());
        this.alarmName.setValue(this.currentAlarm.getItemProperty("name").getValue().toString());
        this.eventType.setValue(this.currentAlarm.getItemProperty("eventType").getValue());
        if (this.eventType.getValue().equals(EventType.JOB_FAILURE)
                && this.currentAlarm.getItemProperty("regexMatch").getValue() != null) {
            this.regexMatch.setValue(this.currentAlarm.getItemProperty("regexMatch").getValue().toString());
        }
        this.severity.setValue(this.currentAlarm.getItemProperty("severity").getValue());

        this.alarmAction.setValue(this.currentAlarm.getItemProperty("alarmAction").getValue());
        if (this.alarmAction.getValue().equals(AlarmAction.EMAIL)) {
            this.email.setValue(this.currentAlarm.getItemProperty("receipientEmail").getValue().toString());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                // creating add alarm with user entered data
                BaseRequest<AlarmDto> request = createRequest();
                request.getDto().setId(this.currentAlarm.getBean().getId());

                log.info("Updating alarm - " + this.alarmName.getValue());

                BaseResponse res = this.updateAlarmService.dispatch(request);
                if (res.getId() == null) {
                    ViewUtil.iscNotification("Error updating the alarm", null, Notification.Type.TRAY_NOTIFICATION);
                }

                // updating bean in the table container
                this.alarmView.getParentContainer().getContainerProperty(request.getDto().getId(), "name")
                        .setValue(this.alarmName.getValue().trim());
                this.alarmView.getParentContainer().getContainerProperty(request.getDto().getId(), "eventType")
                        .setValue(EventType.fromText(this.eventType.getValue().toString().trim()));
                this.alarmView.getParentContainer().getContainerProperty(request.getDto().getId(), "regexMatch")
                        .setValue(this.regexMatch.getValue().trim());
                this.alarmView.getParentContainer().getContainerProperty(request.getDto().getId(), "severity")
                        .setValue(Severity.fromText(this.severity.getValue().toString().trim()));
                this.alarmView.getParentContainer().getContainerProperty(request.getDto().getId(), "alarmAction")
                        .setValue(AlarmAction.fromText(this.alarmAction.getValue().toString().trim()));
                this.alarmView.getParentContainer().getContainerProperty(request.getDto().getId(), "receipientEmail")
                        .setValue(this.email.getValue().trim());
                this.alarmView.getParentContainer().getContainerProperty(request.getDto().getId(), "enabledAlarm")
                        .setValue(this.enabled.getValue());

                close();
            }
        } catch (Exception e) {
            log.info("Error updating alarm", e);
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }

    }
}