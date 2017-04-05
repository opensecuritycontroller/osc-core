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
package org.osc.core.broker.view.vc;

import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CustomTable;
import com.vaadin.ui.CustomTable.ColumnGenerator;
import com.vaadin.ui.Notification;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.model.plugin.sdncontroller.ControllerType;
import org.osc.core.broker.model.virtualization.VirtualizationType;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.GetDtoFromEntityService;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.exceptions.ActionNotSupportedException;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.request.BaseJobRequest;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.GetDtoFromEntityRequest;
import org.osc.core.broker.service.response.BaseDtoResponse;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.service.securitygroup.ListSecurityGroupByVcService;
import org.osc.core.broker.service.securitygroup.SecurityGroupDto;
import org.osc.core.broker.service.securitygroup.SyncSecurityGroupService;
import org.osc.core.broker.service.vc.ListVirtualizationConnectorService;
import org.osc.core.broker.service.vc.SyncVirtualizationConnectorService;
import org.osc.core.broker.util.StaticRegistry;
import org.osc.core.broker.view.CRUDBaseView;
import org.osc.core.broker.view.util.ToolbarButtons;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.view.vc.securitygroup.AddSecurityGroupWindow;
import org.osc.core.broker.view.vc.securitygroup.BindSecurityGroupWindow;
import org.osc.core.broker.view.vc.securitygroup.UpdateSecurityGroupWindow;
import org.osc.core.broker.window.delete.DeleteWindowUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class VirtualizationConnectorView extends CRUDBaseView<VirtualizationConnectorDto, SecurityGroupDto> {

    private static final String VC_HELP_GUID = "GUID-334FD869-4AEE-4CA7-AF22-E0925C623C00.html";
    private static final String SG_HELP_GUID = "GUID-E46D333B-8FDF-4563-AE26-C2D7E3AC5EA9.html";

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(VirtualizationConnectorView.class);

    private final ConformService conformService = StaticRegistry.conformService();

    public VirtualizationConnectorView() {
        createView("Virtualization Connector",
                Arrays.asList(ToolbarButtons.ADD, ToolbarButtons.EDIT, ToolbarButtons.DELETE, ToolbarButtons.CONFORM_VC),
                "Security Group",
                Arrays.asList(ToolbarButtons.ADD_CHILD, ToolbarButtons.EDIT_CHILD, ToolbarButtons.DELETE_CHILD,
                        ToolbarButtons.BIND_SECURITY_GROUP, ToolbarButtons.CONFORM));
    }

    @SuppressWarnings("serial")
    @Override
    public void initParentTable() {
        this.parentContainer = new BeanContainer<>(VirtualizationConnectorDto.class);
        this.parentTable.setContainerDataSource(this.parentContainer);
        this.parentTable.setVisibleColumns("name", "type", "controllerIP", "providerIP", "lastJobStatus");

        this.parentTable.addGeneratedColumn("lastJobStatus", (ColumnGenerator) (source, itemId, columnId) -> {
            VirtualizationConnectorDto vcDto = VirtualizationConnectorView.this.parentContainer.getItem(itemId).getBean();
            return ViewUtil.generateJobLink(vcDto.getLastJobStatus(), vcDto.getLastJobState(), vcDto.getLastJobId());
        });

        this.parentTable.addGeneratedColumn("providerIP", (ColumnGenerator) (source, itemId, columnId) -> {
            VirtualizationConnectorDto vcDto = VirtualizationConnectorView.this.parentContainer.getItem(itemId).getBean();

            return ViewUtil.generateMgrLink("http://", vcDto.getProviderIP(), "", "");
        });

        // Customizing header names
        this.parentTable.setColumnHeader("name", "Name");
        this.parentTable.setColumnHeader("type", "Type");
        this.parentTable.setColumnHeader("controllerIP", "Controller IP");
        this.parentTable.setColumnHeader("providerIP", "Provider IP");
        this.parentTable.setColumnHeader("lastJobStatus", "Last Job Status");
    }

    @Override
    public void populateParentTable() {

        BaseRequest<BaseDto> listRequest = new BaseRequest<>();
        ListResponse<VirtualizationConnectorDto> res;
        ListVirtualizationConnectorService listService = new ListVirtualizationConnectorService();

        try {
            res = listService.dispatch(listRequest);
            List<VirtualizationConnectorDto> listResponse = res.getList();
            this.parentContainer.removeAllItems();
            // Creating table with list of vendors
            for (VirtualizationConnectorDto vcm : listResponse) {
                this.parentContainer.addItem(vcm.getId(), vcm);
            }

        } catch (Exception e) {
            log.error("Fail to populate Virtualization Connector table", e);
            ViewUtil.iscNotification("Fail to populate Virtualization Connector table (" + e.getMessage() + ")",
                    Notification.Type.ERROR_MESSAGE);
        }
    }

    @SuppressWarnings("serial")
    @Override
    public void initChildTable() {
        this.childContainer = new BeanContainer<Long, SecurityGroupDto>(SecurityGroupDto.class);
        this.childTable.setContainerDataSource(this.childContainer);
        this.childTable.setVisibleColumns("name", "tenantName", "memberDescription", "servicesDescription",
                "markForDeletion", "lastJobStatus");
        this.childTable.addGeneratedColumn("lastJobStatus", new ColumnGenerator() {
            @Override
            public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                SecurityGroupDto SGDto = VirtualizationConnectorView.this.childContainer.getItem(itemId).getBean();
                return ViewUtil.generateJobLink(SGDto.getLastJobStatus(), SGDto.getLastJobState(),
                        SGDto.getLastJobId());
            }
        });

        // re-naming table header columns
        this.childTable.setColumnHeader("name", "Name");
        this.childTable.setColumnHeader("tenantName", "Tenant");
        this.childTable.setColumnHeader("memberDescription", "Members");
        this.childTable.setColumnHeader("servicesDescription", "Services");
        this.childTable.setColumnHeader("markForDeletion", "Deleted");
        this.childTable.setColumnHeader("lastJobStatus", "Last Job Status");

    }

    @Override
    public void populateChildTable(BeanItem<VirtualizationConnectorDto> parentItem) {
        if (parentItem != null) {
            try {
                VirtualizationConnectorDto vc = parentItem.getBean();

                if (vc.getType() == VirtualizationType.OPENSTACK) {
                    ViewUtil.enableToolBarButtons(true, this.childToolbar,
                            Arrays.asList(ToolbarButtons.ADD_CHILD.getId()));
                } else {
                    ViewUtil.enableToolBarButtons(false, this.childToolbar,
                            Arrays.asList(ToolbarButtons.ADD_CHILD.getId()));
                }
                this.childContainer.removeAllItems();

                BaseIdRequest idRequest = new BaseIdRequest();
                idRequest.setId(vc.getId());

                ListResponse<SecurityGroupDto> res;
                ListSecurityGroupByVcService listService = new ListSecurityGroupByVcService();
                res = listService.dispatch(idRequest);
                List<SecurityGroupDto> listResponse = res.getList();

                for (SecurityGroupDto securityGroup : listResponse) {
                    this.childContainer.addItem(securityGroup.getId(), securityGroup);
                }

            } catch (Exception e) {
                log.error("Fail to populate Security Group table", e);
                ViewUtil.iscNotification("Fail to populate Security Group table (" + e.getMessage() + ")",
                        Notification.Type.ERROR_MESSAGE);
            }
        } else {
            this.childContainer.removeAllItems();
            ViewUtil.setButtonsEnabled(false, this.childToolbar);
        }
    }

    @Override
    public void buttonClicked(ClickEvent event) throws Exception {
        if (event.getButton().getId().equals(ToolbarButtons.ADD.getId())) {
            ViewUtil.addWindow(new AddVirtualizationConnectorWindow(this));
        }
        if (event.getButton().getId().equals(ToolbarButtons.EDIT.getId())) {
            ViewUtil.addWindow(new UpdateVirtualizationConnectorWindow(this));
        }
        if (event.getButton().getId().equals(ToolbarButtons.DELETE.getId())) {
            DeleteWindowUtil.deleteVirtualizationConnector(getParentItem().getBean());
        }
        if (event.getButton().getId().equals(ToolbarButtons.ADD_CHILD.getId())) {
            VirtualizationConnectorDto vc = getParentItem().getBean();
            if (vc.getControllerType().equals(ControllerType.NONE)) {
                ViewUtil.iscNotification("Creation of Security Groups is not allowed in the absence of SDN Controller.",
                        Notification.Type.ERROR_MESSAGE);
            } else {
                ViewUtil.addWindow(new AddSecurityGroupWindow(getParentItem().getBean()));
            }
        }
        if (event.getButton().getId().equals(ToolbarButtons.EDIT_CHILD.getId())) {
            SecurityGroupDto securityGroup = getChildContainer().getItem(getChildItemId()).getBean();
            boolean markForDeletion = securityGroup.isMarkForDeletion();
            if (markForDeletion) {
                ViewUtil.iscNotification("Modification of deleted Security Group is not allowed.",
                        Notification.Type.WARNING_MESSAGE);
            } else {
                ViewUtil.addWindow(new UpdateSecurityGroupWindow(securityGroup));
            }
        }
        if (event.getButton().getId().equals(ToolbarButtons.DELETE_CHILD.getId())) {
            SecurityGroupDto securityGroup = getChildContainer().getItem(getChildItemId()).getBean();
            DeleteWindowUtil.deleteSecurityGroup(securityGroup);
        }
        if (event.getButton().getId().equals(ToolbarButtons.BIND_SECURITY_GROUP.getId())) {
            SecurityGroupDto securityGroup = getChildContainer().getItem(getChildItemId()).getBean();
            boolean markForDeletion = securityGroup.isMarkForDeletion();
            if (markForDeletion) {
                ViewUtil.iscNotification("Modification of deleted Security Group is not allowed.",
                        Notification.Type.WARNING_MESSAGE);
            } else {
                BindSecurityGroupWindow bindWindow = null;
                try {
                    bindWindow = new BindSecurityGroupWindow(securityGroup);
                    ViewUtil.addWindow(bindWindow);
                } catch (ActionNotSupportedException actionNotSupportedException) {
                    ViewUtil.iscNotification(actionNotSupportedException.getMessage(), Notification.Type.ERROR_MESSAGE);
                    log.warn("Cannot bind Security Group: " + securityGroup.getName(), actionNotSupportedException);
                }
            }
        }

        if (event.getButton().getId().equals(ToolbarButtons.CONFORM.getId())) {
            conformSecurityGroup(getChildItemId(), getParentItemId());
        }

        if (event.getButton().getId().equals(ToolbarButtons.CONFORM_VC.getId())) {
            conformVirtualConnector(getParentItemId());
        }
    }

    private void conformVirtualConnector(Long vcId) {
        log.info("Syncing VC " + vcId.toString());
        BaseJobRequest request = new BaseJobRequest(vcId);
        SyncVirtualizationConnectorService service = new SyncVirtualizationConnectorService();

        try {
            BaseJobResponse response = service.dispatch(request);
            ViewUtil.showJobNotification(response.getJobId());
        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }

    private void conformSecurityGroup(Long sgId, Long vcId) {
        log.info("Syncing Security Group " + sgId.toString());
        SyncSecurityGroupService service = new SyncSecurityGroupService();

        try {
            BaseJobResponse response = service.dispatch(new BaseIdRequest(sgId, vcId));
            ViewUtil.showJobNotification(response.getJobId());

        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }

    @Override
    protected String getParentHelpGuid() {
        return VC_HELP_GUID;
    }

    @Override
    protected String getChildHelpGuid() {
        return SG_HELP_GUID;
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        super.enter(event);
        String parameters = event.getParameters();
        if (!StringUtils.isEmpty(parameters)) {
            Map<String, String> paramMap = ViewUtil.stringToMap(parameters);
            try {
                String vc = paramMap.get(ViewUtil.VC_ID_PARAM_KEY);
                String sg = paramMap.get(ViewUtil.SG_ID_PARAM_KEY);

                Long vcId = null;
                if (vc != null) {
                    vcId = Long.parseLong(vc);
                }
                Long sgId = null;
                if (sg != null) {
                    sgId = Long.parseLong(sg);
                }

                if (sgId != null) {
                    GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
                    getDtoRequest.setEntityId(sgId);
                    getDtoRequest.setEntityName("SecurityGroup");
                    GetDtoFromEntityService<SecurityGroupDto> getSg = new GetDtoFromEntityService<SecurityGroupDto>();

                    try {
                        BaseDtoResponse<SecurityGroupDto> getSgDto = getSg.dispatch(getDtoRequest);
                        vcId = getSgDto.getDto().getParentId();
                    } catch (Exception e) {
                        log.error("Error while getting sg " + sgId + ".");
                    }
                }

                if (vcId != null) {
                    log.info("Entered DA View with Id:" + vcId);
                    this.parentTable.select(vcId);
                    this.parentTable.setCurrentPageFirstItemIndex(getParentContainer().indexOfId(vcId));
                }
                if (sgId != null) {
                    log.info("Entered SG View with Id:" + sgId);
                    this.childTable.select(sgId);
                    this.childTable.setCurrentPageFirstItemIndex(getParentContainer().indexOfId(sgId));
                }

            } catch (Exception ex) {
                log.warn("Invalid Parameters for VC View. " + parameters, ex);
            }
        }
    }

    @Override
    public void parentTableClicked(long itemId) {
        super.parentTableClicked(itemId);
        if (itemId != NULL_SELECTION_ITEM_ID) {
            VirtualizationConnectorDto selected = this.parentContainer.getItem(itemId).getBean();
            setToolbars(selected);
        }
    }

    private void setToolbars(VirtualizationConnectorDto selected) {
        if (selected.getType().isVmware()) {
            // Disable for type VMWARE
            ViewUtil.setButtonsEnabled(false, this.childToolbar);
        } else {
            // Enable SubView Buttons if VS is type VMWARE
            ViewUtil.enableToolBarButtons(true, this.childToolbar, Arrays.asList(ToolbarButtons.ADD.getId()));
        }
    }

    @Override
    public void childTableClicked(long itemId) {
        super.childTableClicked(itemId);
        if (getParentItemId() != NULL_SELECTION_ITEM_ID) {
            VirtualizationConnectorDto selected = this.parentContainer.getItem(getParentItemId()).getBean();
            setToolbars(selected);
        }
    }

}
