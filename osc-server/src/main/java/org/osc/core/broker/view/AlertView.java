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
package org.osc.core.broker.view;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.events.AcknowledgementStatus;
import org.osc.core.broker.service.alert.AcknowledgeAlertService;
import org.osc.core.broker.service.alert.ListAlertService;
import org.osc.core.broker.service.dto.AlertDto;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.request.AlertRequest;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.view.util.ToolbarButtons;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.delete.DeleteWindowUtil;

import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CustomTable;
import com.vaadin.ui.CustomTable.ColumnGenerator;
import com.vaadin.ui.Notification;

public class AlertView extends CRUDBaseView<AlertDto, BaseDto> {

    private static final String ALERT_ID_COLUMN = "id";
    private static final String ALERT_NAME_COLUMN_ID = "name";
    private static final String ALERT_OBJECTS_COLUMN_ID = "object";
    private static final String ALERT_SEVERITY_COLUMN_ID = "severity";
    private static final String ALERT_MESSAGE_COLUMN = "message";
    private static final String ALERT_TIME_CREATED_COLUMN_ID = "timeCreatedTimestamp";
    private static final String ALERT_STATUS_COLUMN_ID = "status";
    private static final String ALERT_TIME_ACKNOWLEDGED_COLUMN_ID = "timeAcknowledgedTimestamp";
    private static final String ALERT_USER_ACKNOWLEDGED_COLUMN_ID = "acknowledgedUser";

    private static final Logger log = Logger.getLogger(AlertView.class);

    private static final long serialVersionUID = 1L;
    private static final String ALERT_HELP_GUID = "GUID-977FE812-0813-41D0-A6A4-28A9E18CD8F6.html";

    public AlertView() {
        super();
        createView("Alerts", Arrays.asList(ToolbarButtons.ACKNOWLEDGE_ALERT, ToolbarButtons.UNACKNOWLEDGE_ALERT,
                ToolbarButtons.DELETE, ToolbarButtons.SHOW_PENDING_ACKNOWLEDGE_ALERTS, ToolbarButtons.SHOW_ALL_ALERTS),
                true);
    }

    @SuppressWarnings("serial")
    @Override
    public void initParentTable() {
        this.parentContainer = new BeanContainer<Long, AlertDto>(AlertDto.class);

        this.parentTable.setContainerDataSource(this.parentContainer);
        this.parentTable.setVisibleColumns(ALERT_ID_COLUMN, ALERT_NAME_COLUMN_ID, ALERT_OBJECTS_COLUMN_ID,
                ALERT_SEVERITY_COLUMN_ID, ALERT_MESSAGE_COLUMN, ALERT_TIME_CREATED_COLUMN_ID, ALERT_STATUS_COLUMN_ID,
                ALERT_TIME_ACKNOWLEDGED_COLUMN_ID, ALERT_USER_ACKNOWLEDGED_COLUMN_ID);

        this.parentTable.addGeneratedColumn(ALERT_OBJECTS_COLUMN_ID, new ColumnGenerator() {
            @Override
            public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                AlertDto alertDto = AlertView.this.parentContainer.getItem(itemId).getBean();
                if (alertDto.getObject() != null) {
                    return ViewUtil.generateObjectLink(alertDto.getObject());
                }
                return null;
            }
        });

        this.parentTable.setColumnHeader(ALERT_ID_COLUMN, "Id");
        this.parentTable.setColumnHeader(ALERT_NAME_COLUMN_ID, "Name");
        this.parentTable.setColumnHeader(ALERT_OBJECTS_COLUMN_ID, "Objects");
        this.parentTable.setColumnHeader(ALERT_SEVERITY_COLUMN_ID, "Severity");
        this.parentTable.setColumnHeader(ALERT_MESSAGE_COLUMN, "Message");
        this.parentTable.setColumnHeader(ALERT_TIME_CREATED_COLUMN_ID, "Created");
        this.parentTable.setColumnHeader(ALERT_STATUS_COLUMN_ID, "Status");
        this.parentTable.setColumnHeader(ALERT_TIME_ACKNOWLEDGED_COLUMN_ID, "Acknowledged");
        this.parentTable.setColumnHeader(ALERT_USER_ACKNOWLEDGED_COLUMN_ID, "Acknowledged By");
    }

    @Override
    public void populateParentTable() {

        this.parentContainer.removeAllItems();
        ListAlertService listService = new ListAlertService();

        try {
            ListResponse<AlertDto> res = listService.dispatch(new BaseRequest<>());
            List<AlertDto> listResponse = res.getList();
            for (AlertDto dto : listResponse) {
                this.parentContainer.addItem(dto.getId(), dto);
            }

        } catch (Exception e) {
            log.error("Failed to populate Alert table", e);
            ViewUtil.iscNotification("Failed to populate Alert table (" + e.getMessage() + ")",
                    Notification.Type.ERROR_MESSAGE);
        }

        // Only show unacknowledged alerts when landed on this view
        showPendingAcknowledgeAlerts();
    }

    @Override
    public void initChildTable() {
    }

    @Override
    public void populateChildTable(BeanItem<AlertDto> parentItem) {
    }

    @Override
    public void buttonClicked(ClickEvent event) {
        if (event.getButton().getId().equals(ToolbarButtons.ACKNOWLEDGE_ALERT.getId())) {
            acknowledgeAlert(true);
        } else if (event.getButton().getId().equals(ToolbarButtons.UNACKNOWLEDGE_ALERT.getId())) {
            acknowledgeAlert(false);
        } else if (event.getButton().getId().equals(ToolbarButtons.DELETE.getId())) {
            deleteAlert();
        } else if (event.getButton().getId().equals(ToolbarButtons.SHOW_PENDING_ACKNOWLEDGE_ALERTS.getId())) {
            showPendingAcknowledgeAlerts();
        } else if (event.getButton().getId().equals(ToolbarButtons.SHOW_ALL_ALERTS.getId())) {
            showAllAlerts();

        }
    }

    private void acknowledgeAlert(boolean acknowledge) {
        try {
            AlertRequest req = new AlertRequest();
            req.setDtoList(this.itemList);
            req.setAcknowledge(acknowledge);
            AcknowledgeAlertService acknowledgeService = new AcknowledgeAlertService();
            acknowledgeService.dispatch(req);
            log.info("Acknowledge/Unacknowledge Alert(s) Successful!");
        } catch (Exception e) {
            log.error("Failed to acknowledge/unacknowledge alert(s)", e);
            ViewUtil.iscNotification("Failed to acknowledge/unacknowledge alert(s).", Notification.Type.WARNING_MESSAGE);
        }
    }

    private void deleteAlert() {
        DeleteWindowUtil.deleteAlert(this.itemList);
    }

    private void showPendingAcknowledgeAlerts() {
        this.parentTable.resetFilters();
        this.parentTable.setFilterFieldValue(ALERT_STATUS_COLUMN_ID, AcknowledgementStatus.PENDING_ACKNOWLEDGEMENT);
    }

    private void showAllAlerts() {
        this.parentTable.resetFilters();
        this.parentTable.setFilterFieldValue(ALERT_STATUS_COLUMN_ID, null);
    }

    @Override
    protected String getParentHelpGuid() {
        return ALERT_HELP_GUID;
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        super.enter(event);
        String parameters = event.getParameters();
        if (!StringUtils.isEmpty(parameters)) {
            Map<String, String> paramMap = ViewUtil.stringToMap(parameters);
            try {
                Long alertId = Long.parseLong(paramMap.get(ViewUtil.ALERT_ID_PARAM_KEY));
                log.info("Entered Alert View with Id:" + alertId);
                this.parentTable.setValue(null);
                this.parentTable.select(alertId);
                this.parentTable.setCurrentPageFirstItemIndex(getParentContainer().indexOfId(alertId));
            } catch (NumberFormatException ne) {
                log.warn("Invalid Parameters for Alert View. " + parameters);
            }
        }
    }
}
