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
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.ListApplianceService;
import org.osc.core.broker.service.ListApplianceSoftwareVersionService;
import org.osc.core.broker.service.dto.ApplianceDto;
import org.osc.core.broker.service.dto.ApplianceSoftwareVersionDto;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.ListApplianceSoftwareVersionRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.util.ToolbarButtons;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.add.ImportApplianceSoftwareVersionWindow;
import org.osc.core.broker.window.delete.DeleteApplianceSoftwareVersionWindow;
import org.osc.core.broker.window.delete.DeleteApplianceWindow;
import org.osc.core.util.ServerUtil;

import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Notification;

@SuppressWarnings("serial")
public class ApplianceView extends CRUDBaseView<ApplianceDto, ApplianceSoftwareVersionDto> {

    private static final String APPLIANCE_HELP_GUID = "GUID-34E04177-4993-4072-B43D-FC70C8B94E04.html";
    private static final Logger log = Logger.getLogger(ApplianceView.class);

    public ApplianceView() {

        createView("Model", Arrays.asList(ToolbarButtons.DELETE, ToolbarButtons.AUTO_IMPORT_APPLIANCE),
                "Software Version", Arrays.asList(ToolbarButtons.DELETE_CHILD));

        ViewUtil.getButtonById(this.parentToolbar, ToolbarButtons.AUTO_IMPORT_APPLIANCE.getId()).setEnabled(true);
    }

    @Override
    public void buttonClicked(ClickEvent event) throws Exception {

        if (event.getButton().getId().equals(ToolbarButtons.DELETE.getId())) {
            log.info("Redirecting to Delete Appliance Window");
            ViewUtil.addWindow(new DeleteApplianceWindow(this));
        }

        if (event.getButton().getId().equals(ToolbarButtons.AUTO_IMPORT_APPLIANCE.getId())) {
            try {
                if (!ServerUtil.isEnoughSpace()) {
                    throw new VmidcException(VmidcMessages.getString("upload.appliance.nospace"));
                }

                log.info("Redirecting to Add Appliance Version Window");
                ViewUtil.addWindow(new ImportApplianceSoftwareVersionWindow());

            } catch (Exception e) {
                log.error("Failed to initiate adding a new Appliance Software Version", e);
                ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
            }
        }

        if (event.getButton().getId().equals(ToolbarButtons.DELETE_CHILD.getId())) {
            log.info("Redirecting to Delete Appliance Version Window");
            ViewUtil.addWindow(new DeleteApplianceSoftwareVersionWindow(this));
        }

    }

    @Override
    public void initParentTable() {
        this.parentContainer = new BeanContainer<Long, ApplianceDto>(ApplianceDto.class);
        this.parentTable.setContainerDataSource(this.parentContainer);
        this.parentTable.setVisibleColumns("model", "managerType", "managerVersion");

        // renaming column header
        this.parentTable.setColumnHeader("model", "Model");
        this.parentTable.setColumnHeader("managerType", "Manager Type");
        this.parentTable.setColumnHeader("managerVersion", "Manager Software Version");
    }

    @Override
    public void populateParentTable() {

        BaseRequest<BaseDto> listRequest = null;
        ListResponse<ApplianceDto> res;
        ListApplianceService listService = new ListApplianceService();
        try {
            res = listService.dispatch(listRequest);
            List<ApplianceDto> listResponse = res.getList();
            this.parentContainer.removeAllItems();
            // creating table with list of vendors
            for (ApplianceDto appliance : listResponse) {
                this.parentContainer.addItem(appliance.getId(), appliance);
            }

        } catch (Exception e) {
            log.error("Fail to populate Appliance table", e);
            ViewUtil.iscNotification("Fail to populate Appliance table (" + e.getMessage() + ")",
                    Notification.Type.ERROR_MESSAGE);
        }

    }

    @Override
    public void initChildTable() {
        this.childContainer = new BeanContainer<Long, ApplianceSoftwareVersionDto>(ApplianceSoftwareVersionDto.class);
        this.childTable.setContainerDataSource(this.childContainer);
        this.childTable.setVisibleColumns("swVersion", "virtualizationType", "imageUrl");

        // re-naming table header columns
        this.childTable.setColumnHeader("swVersion", "Software");
        this.childTable.setColumnHeader("virtualizationType", "Virtualization Type");
        this.childTable.setColumnHeader("imageUrl", "Image Name");
    }

    @Override
    public void populateChildTable(BeanItem<ApplianceDto> parentItem) {
        if (parentItem != null) {
            try {
                ApplianceDto ap = parentItem.getBean();
                ListApplianceSoftwareVersionRequest listRequest = new ListApplianceSoftwareVersionRequest();
                listRequest.setApplianceId(ap.getId());
                ListResponse<ApplianceSoftwareVersionDto> res;
                ListApplianceSoftwareVersionService listService = new ListApplianceSoftwareVersionService();
                this.childContainer.removeAllItems();
                res = listService.dispatch(listRequest);
                List<ApplianceSoftwareVersionDto> listResponse = res.getList();
                for (ApplianceSoftwareVersionDto aVersion : listResponse) {
                    this.childContainer.addItem(aVersion.getId(), aVersion);
                }

            } catch (Exception e) {
                log.error("Fail to populate Appliance Software Version table", e);
                ViewUtil.iscNotification("Fail to populate Appliance Software Version table (" + e.getMessage()
                        + ")", Notification.Type.ERROR_MESSAGE);
            }
        } else {
            this.childContainer.removeAllItems();
            ViewUtil.setButtonsEnabled(false, this.childToolbar, Collections.<String>emptyList());
        }
    }

    @Override
    protected String getParentHelpGuid() {
        return APPLIANCE_HELP_GUID;
    }
}
