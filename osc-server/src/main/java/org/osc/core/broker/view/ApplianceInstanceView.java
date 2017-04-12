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
import org.osc.core.broker.service.GetAgentStatusService;
import org.osc.core.broker.service.ListDistributedApplianceInstanceService;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.dto.DistributedApplianceInstanceDto;
import org.osc.core.broker.service.dto.job.LockObjectDto;
import org.osc.core.broker.service.dto.job.ObjectType;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ToolbarButtons;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.status.AgentStatusWindow;

import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CustomTable;
import com.vaadin.ui.CustomTable.ColumnGenerator;
import com.vaadin.ui.Notification;

public class ApplianceInstanceView extends CRUDBaseView<DistributedApplianceInstanceDto, BaseDto> {

    private static final String DAI_HELP_GUID = "GUID-E4986D6E-C481-43C4-8801-670CAF8C1581.html";

    private static final long serialVersionUID = 1605215705219327527L;

    private static final Logger LOG = Logger.getLogger(ApplianceInstanceView.class);

    private GetAgentStatusService getAgentStatusService;

    public ApplianceInstanceView() {
        super();
        createView(VmidcMessages.getString(VmidcMessages_.DAI_TITLE), Arrays.asList(ToolbarButtons.APPLIANCE_STATUS), true);
        setInfoText(VmidcMessages.getString(VmidcMessages_.PAGE_INFO_HELP_TITLE),
                VmidcMessages.getString(VmidcMessages_.DAI_HELP_MESSAGE));
    }

    @SuppressWarnings("serial")
    @Override
    public void initParentTable() {
        this.parentContainer = new BeanContainer<Long, DistributedApplianceInstanceDto>(
                DistributedApplianceInstanceDto.class);

        this.parentTable.setContainerDataSource(this.parentContainer);
        this.parentTable.setVisibleColumns("name", "ipAddress", "discovered", "inspectionReady", "lastStatus",
                "hostname", "virtualConnectorName", "applianceManagerConnectorName",
                "distributedApplianceName", "applianceModel", "swVersion");

        this.parentTable.addGeneratedColumn("applianceManagerConnectorName", new ColumnGenerator() {
            @Override
            public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                DistributedApplianceInstanceDto daiDto = ApplianceInstanceView.this.parentContainer
                        .getItem(itemId).getBean();
                return ViewUtil.generateObjectLink(new LockObjectDto(daiDto.getMcId(),
                        daiDto.getApplianceManagerConnectorName(),
                        ObjectType.APPLIANCE_MANAGER_CONNECTOR));
            }
        });

        this.parentTable.addGeneratedColumn("virtualConnectorName", new ColumnGenerator() {
            @Override
            public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                DistributedApplianceInstanceDto daiDto = ApplianceInstanceView.this.parentContainer
                        .getItem(itemId).getBean();
                return ViewUtil.generateObjectLink(new LockObjectDto(daiDto.getVcId(),
                        daiDto.getVirtualConnectorName(),
                        ObjectType.VIRTUALIZATION_CONNECTOR));
            }
        });

        this.parentTable.addGeneratedColumn("distributedApplianceName", new ColumnGenerator() {
            @Override
            public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                DistributedApplianceInstanceDto daiDto = ApplianceInstanceView.this.parentContainer
                        .getItem(itemId).getBean();
                return ViewUtil.generateObjectLink(new LockObjectDto(daiDto.getVirtualsystemId(),
                        daiDto.getDistributedApplianceName(),
                        ObjectType.VIRTUAL_SYSTEM));
            }
        });

        this.parentTable.setColumnHeader("name", "Name");
        this.parentTable.setColumnHeader("ipAddress", "IP-Address");
        this.parentTable.setColumnHeader("discovered", "Discovered");
        this.parentTable.setColumnHeader("inspectionReady", "Inspection-Ready");
        this.parentTable.setColumnHeader("lastStatus", "Last Status");
        this.parentTable.setColumnHeader("hostname", "V.Server");
        this.parentTable.setColumnHeader("virtualConnectorName", "V.Connector");
        this.parentTable.setColumnHeader("applianceManagerConnectorName", "Manager");
        this.parentTable.setColumnHeader("distributedApplianceName", "Distributed Appliance");
        this.parentTable.setColumnHeader("applianceModel", "Model");
        this.parentTable.setColumnHeader("swVersion", "Version");
    }

    @Override
    public void populateParentTable() {

        this.parentContainer.removeAllItems();
        ListDistributedApplianceInstanceService listService = new ListDistributedApplianceInstanceService();

        try {
            ListResponse<DistributedApplianceInstanceDto> res = listService.dispatch(new BaseRequest<>());
            List<DistributedApplianceInstanceDto> listResponse = res.getList();
            for (DistributedApplianceInstanceDto dto : listResponse) {
                this.parentContainer.addItem(dto.getId(), dto);
            }

        } catch (Exception e) {
            LOG.error("Fail to populate DAI table", e);
            ViewUtil.iscNotification("Fail to populate Appliance Instance table (" + e.getMessage() + ")",
                    Notification.Type.ERROR_MESSAGE);
        }
    }

    @Override
    public void initChildTable() {
    }

    @Override
    public void populateChildTable(BeanItem<DistributedApplianceInstanceDto> parentItem) {
    }

    @Override
    public void buttonClicked(ClickEvent event) {
        if (event.getButton().getId().equals(ToolbarButtons.APPLIANCE_STATUS.getId())) {
            checkApplianceInstanceStatus();
        }
    }

    private void checkApplianceInstanceStatus() {
        try {
            ViewUtil.addWindow(new AgentStatusWindow(this.itemList, this.getAgentStatusService));
        } catch (Exception e) {
            LOG.error("Failed to get status from Agent(s)", e);
            ViewUtil.iscNotification("Failed to get status from Agent(s).", Notification.Type.WARNING_MESSAGE);
        }
    }

    @Override
    protected String getParentHelpGuid() {
        return DAI_HELP_GUID;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void updateParentContainer(DistributedApplianceInstanceDto dto) {
        DistributedApplianceInstanceDto updateDto = dto;

        this.parentContainer.getItem(dto.getId()).getItemProperty("ipAddress").setValue(updateDto.getIpAddress());
        this.parentContainer.getItem(dto.getId()).getItemProperty("discovered")
        .setValue(updateDto.getDiscovered());
        this.parentContainer.getItem(dto.getId()).getItemProperty("inspectionReady")
        .setValue(updateDto.getInspectionReady());
        this.parentContainer.getItem(dto.getId()).getItemProperty("lastStatus")
        .setValue(updateDto.getLastStatus());
        this.parentContainer.getItem(dto.getId()).getItemProperty("hostname").setValue(updateDto.getHostname());
        this.parentContainer.getItem(dto.getId()).getItemProperty("virtualConnectorName")
        .setValue(updateDto.getVirtualConnectorName());
        this.parentContainer.getItem(dto.getId()).getItemProperty("applianceManagerConnectorName")
        .setValue(updateDto.getApplianceManagerConnectorName());
        this.parentContainer.getItem(dto.getId()).getItemProperty("distributedApplianceName")
        .setValue(updateDto.getDistributedApplianceName());
        this.parentContainer.getItem(dto.getId()).getItemProperty("applianceModel").setValue(updateDto.getApplianceModel());
        this.parentContainer.getItem(dto.getId()).getItemProperty("swVersion").setValue(updateDto.getSwVersion());
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        super.enter(event);
        String parameters = event.getParameters();
        if (!StringUtils.isEmpty(parameters)) {
            Map<String, String> paramMap = ViewUtil.stringToMap(parameters);
            try {
                Long daiId = Long.parseLong(paramMap.get(ViewUtil.DAI_ID_PARAM_KEY));
                LOG.info("Entered DAI View with Id:" + daiId);
                this.parentTable.setValue(null);
                this.parentTable.select(daiId);
                this.parentTable.setCurrentPageFirstItemIndex(getParentContainer().indexOfId(daiId));
            } catch (NumberFormatException ne) {
                LOG.warn("Invalid Parameters for DAI View. " + parameters);
            }
        }
    }

}
