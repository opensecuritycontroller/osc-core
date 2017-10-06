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

import java.util.Arrays;
import java.util.List;

import org.osc.core.broker.service.api.AddAlarmServiceApi;
import org.osc.core.broker.service.api.DeleteAlarmServiceApi;
import org.osc.core.broker.service.api.GetEmailSettingsServiceApi;
import org.osc.core.broker.service.api.ListAlarmServiceApi;
import org.osc.core.broker.service.api.UpdateAlarmServiceApi;
import org.osc.core.broker.service.dto.AlarmDto;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.view.CRUDBaseView;
import org.osc.core.broker.view.util.ToolbarButtons;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.delete.DeleteWindowUtil;
import org.osc.core.ui.LogProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;

import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Notification;

@Component(service={AlarmView.class}, scope=ServiceScope.PROTOTYPE)
public class AlarmView extends CRUDBaseView<AlarmDto, BaseDto> {

    private static final String ALARM_HELP_GUID = "GUID-98B127AC-2A18-4537-B0FA-CA3DDD4A733C.html";

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Logger log = LogProvider.getLogger(AlarmView.class);

    @Reference
    private AddAlarmServiceApi addAlarmService;

    @Reference
    private UpdateAlarmServiceApi updateAlarmService;

    @Reference
    private DeleteAlarmServiceApi deleteAlarmService;

    @Reference
    private ListAlarmServiceApi listAlarmService;

    @Reference
    private GetEmailSettingsServiceApi getEmailSettingsService;

    @Activate
    private void activate() {
        createView("Alarms", Arrays.asList(ToolbarButtons.ADD, ToolbarButtons.EDIT, ToolbarButtons.DELETE));
    }

    @Override
    public void buttonClicked(ClickEvent event) throws Exception {
        if (event.getButton().getId().equals(ToolbarButtons.ADD.getId())) {
            log.debug("Redirecting to Add Alarm Window");
            ViewUtil.addWindow(new AddAlarmWindow(this, this.addAlarmService,
                    this.getEmailSettingsService));
        }
        if (event.getButton().getId().equals(ToolbarButtons.EDIT.getId())) {
            log.debug("Redirecting to Update Alarm Window");
            ViewUtil.addWindow(new UpdateAlarmWindow(this, this.updateAlarmService,
                    this.getEmailSettingsService));
        }
        if (event.getButton().getId().equals(ToolbarButtons.DELETE.getId())) {
            log.debug("Redirecting to Delete Alarm Window");
            DeleteWindowUtil.deleteAlarm(this.deleteAlarmService, getParentItem().getBean());
        }
    }

    @Override
    public void initParentTable() {
        this.parentContainer = new BeanContainer<Long, AlarmDto>(AlarmDto.class);
        this.parentTable.setContainerDataSource(this.parentContainer);
        this.parentTable.setVisibleColumns("name", "enabledAlarm", "eventType", "severity", "alarmAction");

        // Customizing header names
        this.parentTable.setColumnHeader("name", "Name");
        this.parentTable.setColumnHeader("enabledAlarm", "Enabled");
        this.parentTable.setColumnHeader("eventType", "Event Type");
        this.parentTable.setColumnHeader("severity", "Severity");
        this.parentTable.setColumnHeader("alarmAction", "Action");
    }

    @Override
    public void populateParentTable() {

        BaseIdRequest listRequest = new BaseIdRequest();

        try {
            ListResponse<AlarmDto> res = this.listAlarmService.dispatch(listRequest);
            List<AlarmDto> listResponse = res.getList();

            // Creating table with list of alarms
            for (AlarmDto alarm : listResponse) {
                this.parentContainer.addItem(alarm.getId(), alarm);
            }

        } catch (Exception e) {
            log.error("Failed to populate Alarm table", e);
            ViewUtil.iscNotification("Fail to populate Alarm table (" + e.getMessage() + ")",
                    Notification.Type.ERROR_MESSAGE);
        }
    }

    @Override
    public void initChildTable() {
        // not needed in this View

    }

    @Override
    public void populateChildTable(BeanItem<AlarmDto> parentItem) {
        // not needed in this view

    }

    @Override
    protected String getParentHelpGuid() {
        return ALARM_HELP_GUID;
    }
}
