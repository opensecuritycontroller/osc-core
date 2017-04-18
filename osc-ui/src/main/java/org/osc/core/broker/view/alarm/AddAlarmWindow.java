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
import org.osc.core.broker.service.api.AddAlarmServiceApi;
import org.osc.core.broker.service.api.GetEmailSettingsServiceApi;
import org.osc.core.broker.service.dto.AlarmDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.view.util.ViewUtil;

import com.vaadin.ui.Notification;

public class AddAlarmWindow extends BaseAlarmWindow {

    private static final long serialVersionUID = 1L;

    final String CAPTION = "Add Alarm";

    private static final Logger log = Logger.getLogger(AddAlarmWindow.class);

    private AddAlarmServiceApi addAlarmService;

    public AddAlarmWindow(AlarmView alarmView, AddAlarmServiceApi addAlarmService,
            GetEmailSettingsServiceApi getEmailSettingsService) throws Exception {

        super(getEmailSettingsService);
        this.alarmView = alarmView;
        this.addAlarmService = addAlarmService;
        createWindow(this.CAPTION);
    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                // creating add request with user entered data
                BaseRequest<AlarmDto> request = createRequest();

                // calling add service
                BaseResponse addResponse = this.addAlarmService.dispatch(request);
                log.info("adding new alarm - " + this.alarmName.getValue());

                // adding returned ID to the request DTO object
                request.getDto().setId(addResponse.getId());
                // adding new object to the parent table
                this.alarmView.getParentContainer().addItemAt(0, request.getDto().getId(), request.getDto());
                this.alarmView.parentTableClicked(request.getDto().getId());
                close();
            }
        } catch (Exception e) {
            log.info(e.getMessage());
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }

    }
}