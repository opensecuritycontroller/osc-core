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
package org.osc.core.broker.view.securityinterface;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.AddSecurityGroupInterfaceServiceApi;
import org.osc.core.broker.service.api.DeleteSecurityGroupInterfaceServiceApi;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceApi;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceFactoryApi;
import org.osc.core.broker.service.api.ListSecurityGroupInterfaceServiceByVirtualSystemApi;
import org.osc.core.broker.service.api.ListVirtualSystemPolicyServiceApi;
import org.osc.core.broker.service.api.UpdateSecurityGroupInterfaceServiceApi;
import org.osc.core.broker.service.dto.ApplianceManagerConnectorDto;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.dto.SecurityGroupInterfaceDto;
import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.osc.core.broker.service.dto.VirtualizationType;
import org.osc.core.broker.service.dto.job.LockObjectDto;
import org.osc.core.broker.service.dto.job.ObjectTypeDto;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.request.GetDtoFromEntityRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.view.CRUDBaseSubView;
import org.osc.core.broker.view.CRUDBaseView;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ToolbarButtons;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.delete.DeleteWindowUtil;

import com.vaadin.data.util.BeanContainer;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CustomTable;
import com.vaadin.ui.CustomTable.ColumnGenerator;

@SuppressWarnings("serial")
public class SecurityGroupInterfaceSubView extends CRUDBaseSubView<VirtualSystemDto, SecurityGroupInterfaceDto> {

    private static final String SECURITY_GROUP_INTERFACE_HELP_GUID = "GUID-C6F07F61-5669-4085-AF20-07212408C984.html";
    private static final String MC_ENTITY_NAME = "ApplianceManagerConnector";

    private static final Logger log = Logger.getLogger(SecurityGroupInterfaceSubView.class);

    private final AddSecurityGroupInterfaceServiceApi addSecurityGroupInterfaceService;
    private final DeleteSecurityGroupInterfaceServiceApi deleteSecurityGroupInterfaceService;
    private final ListSecurityGroupInterfaceServiceByVirtualSystemApi listSecurityGroupInterfaceServiceByVirtualSystem;
    private final ListVirtualSystemPolicyServiceApi listVirtualSystemPolicyService;
    private final UpdateSecurityGroupInterfaceServiceApi updateSecurityGroupInterfaceService;
    private final GetDtoFromEntityServiceFactoryApi getDtoFromEntityServiceFactory;

    public SecurityGroupInterfaceSubView(String title, ToolbarButtons[] buttons, CRUDBaseView<?, ?> currentView,
            VirtualSystemDto vs, AddSecurityGroupInterfaceServiceApi addSecurityGroupInterfaceService,
            DeleteSecurityGroupInterfaceServiceApi deleteSecurityGroupInterfaceService,
            ListSecurityGroupInterfaceServiceByVirtualSystemApi listSecurityGroupInterfaceServiceByVirtualSystem,
            ListVirtualSystemPolicyServiceApi listVirtualSystemPolicyService,
            UpdateSecurityGroupInterfaceServiceApi updateSecurityGroupInterfaceService,
            GetDtoFromEntityServiceFactoryApi getDtoFromEntityServiceFactory) throws Exception {
        super(currentView, title, buttons, vs);
        this.addSecurityGroupInterfaceService = addSecurityGroupInterfaceService;
        this.deleteSecurityGroupInterfaceService = deleteSecurityGroupInterfaceService;
        this.listVirtualSystemPolicyService = listVirtualSystemPolicyService;
        this.listSecurityGroupInterfaceServiceByVirtualSystem = listSecurityGroupInterfaceServiceByVirtualSystem;
        this.updateSecurityGroupInterfaceService = updateSecurityGroupInterfaceService;
        this.getDtoFromEntityServiceFactory = getDtoFromEntityServiceFactory;

        if (!vs.isMarkForDeletion()) {
            ViewUtil.enableToolBarButtons(
                    ((VirtualSystemDto) this.parent).getVirtualizationType() != VirtualizationType.VMWARE && isPolicyMappingSupported(),
                    this.toolbar, Arrays.asList(ToolbarButtons.ADD.getId()));
        } else {
            //Disable CRUD buttons
            ViewUtil.setButtonsEnabled(false, this.toolbar, Arrays.asList(ToolbarButtons.BACK.getId()));

            this.table.setEnabled(false);
        }
    }

    private boolean isPolicyMappingSupported() throws Exception {
        DistributedApplianceDto daDto = (DistributedApplianceDto)this.currentView.getParentItem().getBean();
        GetDtoFromEntityRequest getMcRequest = new GetDtoFromEntityRequest();
        getMcRequest.setEntityId(daDto.getMcId());
        getMcRequest.setEntityName(MC_ENTITY_NAME);
        GetDtoFromEntityServiceApi<ApplianceManagerConnectorDto> getMcService = this.getDtoFromEntityServiceFactory.getService(ApplianceManagerConnectorDto.class);
        ApplianceManagerConnectorDto mcDto = (getMcService.dispatch(getMcRequest).getDto());
        return mcDto.isPolicyMappingSupported();
    }

    @Override
    public void buttonClicked(ClickEvent event) throws Exception {
        if (event.getButton().getId().equals(ToolbarButtons.ADD.getId())) {
            log.debug("Redirecting to Add Security Group interface Window");
            ViewUtil.addWindow(new AddSecurityGroupInterfaceWindow(getDtoInContext().getId(), this.addSecurityGroupInterfaceService, this.listVirtualSystemPolicyService));
        }
        if (event.getButton().getId().equals(ToolbarButtons.EDIT.getId())) {
            log.debug("Redirecting to Update Security Group interface Window");
            ViewUtil.addWindow(new UpdateSecurityGroupInterfaceWindow(getSelectedItem().getBean(),
                    this.listVirtualSystemPolicyService, this.updateSecurityGroupInterfaceService));
        }
        if (event.getButton().getId().equals(ToolbarButtons.DELETE.getId())) {
            log.debug("Redirecting to Delete Security Group interface Window");
            DeleteWindowUtil.deleteSecurityGroupInterface(getSelectedItem().getBean(), this.deleteSecurityGroupInterfaceService);
        }
        if (event.getButton().getId().equals(ToolbarButtons.BACK.getId())) {
            // removing object from the sub view map so it can be garbage collected
            this.currentView.childSubViewMap.put(this.currentView.getKeyforChildSubView(2), null);
            this.currentView.viewSplitter.removeComponent(this.currentView.viewSplitter.getSecondComponent());
            this.currentView.viewSplitter.addComponent(this.currentView.childContainerLayout);
        }
    }

    @Override
    public void initTable() {
        this.tableContainer = new BeanContainer<Long, SecurityGroupInterfaceDto>(SecurityGroupInterfaceDto.class);
        this.table.setContainerDataSource(this.tableContainer);
        this.table.setVisibleColumns("name", "policyName", "tagValue", "userConfigurable", "securityGroupName",
                "failurePolicyType", "markForDeletion");

        this.table.addGeneratedColumn("securityGroupName", new ColumnGenerator() {
            @Override
            public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                SecurityGroupInterfaceDto dto = SecurityGroupInterfaceSubView.this.tableContainer.getItem(itemId)
                        .getBean();
                if (dto.getSecurityGroupId() != null) {
                    return ViewUtil.generateObjectLink(new LockObjectDto(dto.getSecurityGroupId(), dto
                            .getSecurityGroupName(), ObjectTypeDto.SECURITY_GROUP));
                } else {
                    return null;
                }
            }
        });

        this.table.addGeneratedColumn("policyName", new ColumnGenerator() {
            @Override
            public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                SecurityGroupInterfaceDto dto = SecurityGroupInterfaceSubView.this.tableContainer.getItem(itemId)
                        .getBean();
                if (dto.getPolicyName() == null) {
                    return VmidcMessages.getString(VmidcMessages_.TEXT_NOTAPPLICABLE);
                }
                return dto.getPolicyName();
            }
        });

        this.table.addGeneratedColumn("tagValue", new ColumnGenerator() {
            @Override
            public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                SecurityGroupInterfaceDto dto = SecurityGroupInterfaceSubView.this.tableContainer.getItem(itemId)
                        .getBean();
                if (dto.getTagValue() == null) {
                    return VmidcMessages.getString(VmidcMessages_.TEXT_NOTAPPLICABLE);
                }
                return dto.getTagValue();
            }
        });

        // Customizing column header names
        this.table.setColumnHeader("name", "Name");
        this.table.setColumnHeader("policyName", "Inspection Policy");
        this.table.setColumnHeader("tagValue", "Tag");
        this.table.setColumnHeader("userConfigurable", "User-Defined");
        this.table.setColumnHeader("securityGroupName", "Security Group");
        this.table.setColumnHeader("failurePolicyType", "Failure Policy");
        this.table.setColumnHeader("markForDeletion", "Deleted");
    }

    @Override
    public void populateTable() {
        try {

            BaseIdRequest request = new BaseIdRequest();
            request.setId(getDtoInContext().getId());
            ListResponse<SecurityGroupInterfaceDto> res = this.listSecurityGroupInterfaceServiceByVirtualSystem.dispatch(request);

            this.tableContainer.removeAllItems();
            for (SecurityGroupInterfaceDto sgi : res.getList()) {
                this.tableContainer.addItem(sgi.getId(), sgi);
            }
        } catch (Exception e) {
            log.error("Failed to populate Security Group interfaces", e);
        }
    }

    @Override
    protected void tableClicked(long selectedItemId) {
        super.tableClicked(selectedItemId);
        if (selectedItemId != NULL_SELECTION_ITEM_ID) {
            SecurityGroupInterfaceDto selectedSgi = this.tableContainer.getItem(selectedItemId).getBean();
            ViewUtil.enableToolBarButtons(selectedSgi.isUserConfigurable(), this.toolbar,
                    Arrays.asList(ToolbarButtons.EDIT.getId(), ToolbarButtons.DELETE.getId()));
        }
    }

    @Override
    protected String getSubViewHelpGuid() {
        return SECURITY_GROUP_INTERFACE_HELP_GUID;
    }

}
