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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.osc.core.broker.service.api.AddDeploymentSpecServiceApi;
import org.osc.core.broker.service.api.AddDistributedApplianceServiceApi;
import org.osc.core.broker.service.api.AddSecurityGroupInterfaceServiceApi;
import org.osc.core.broker.service.api.DeleteDeploymentSpecServiceApi;
import org.osc.core.broker.service.api.DeleteDistributedApplianceServiceApi;
import org.osc.core.broker.service.api.DeleteSecurityGroupInterfaceServiceApi;
import org.osc.core.broker.service.api.ForceDeleteVirtualSystemServiceApi;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceApi;
import org.osc.core.broker.service.api.GetDtoFromEntityServiceFactoryApi;
import org.osc.core.broker.service.api.ListApplianceManagerConnectorServiceApi;
import org.osc.core.broker.service.api.ListApplianceModelSwVersionComboServiceApi;
import org.osc.core.broker.service.api.ListAvailabilityZonesServiceApi;
import org.osc.core.broker.service.api.ListDeploymentSpecServiceByVirtualSystemApi;
import org.osc.core.broker.service.api.ListDistributedApplianceServiceApi;
import org.osc.core.broker.service.api.ListDomainsByMcIdServiceApi;
import org.osc.core.broker.service.api.ListEncapsulationTypeByVersionTypeAndModelApi;
import org.osc.core.broker.service.api.ListFloatingIpPoolsServiceApi;
import org.osc.core.broker.service.api.ListHostAggregateServiceApi;
import org.osc.core.broker.service.api.ListHostServiceApi;
import org.osc.core.broker.service.api.ListNetworkServiceApi;
import org.osc.core.broker.service.api.ListProjectServiceApi;
import org.osc.core.broker.service.api.ListRegionServiceApi;
import org.osc.core.broker.service.api.ListSecurityGroupInterfaceServiceByVirtualSystemApi;
import org.osc.core.broker.service.api.ListVirtualSystemPolicyServiceApi;
import org.osc.core.broker.service.api.ListVirtualizationConnectorBySwVersionServiceApi;
import org.osc.core.broker.service.api.SyncDeploymentSpecServiceApi;
import org.osc.core.broker.service.api.SyncDistributedApplianceServiceApi;
import org.osc.core.broker.service.api.UpdateDeploymentSpecServiceApi;
import org.osc.core.broker.service.api.UpdateDistributedApplianceServiceApi;
import org.osc.core.broker.service.api.UpdateSecurityGroupInterfaceServiceApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.api.server.ValidationApi;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.dto.SecurityGroupInterfaceDto;
import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.osc.core.broker.service.dto.job.LockObjectDto;
import org.osc.core.broker.service.dto.job.ObjectTypeDto;
import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.GetDtoFromEntityRequest;
import org.osc.core.broker.service.response.BaseDtoResponse;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.view.deploymentspec.DeploymentSpecSubView;
import org.osc.core.broker.view.securityinterface.SecurityGroupInterfaceSubView;
import org.osc.core.broker.view.util.ToolbarButtons;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.add.AddDistributedApplianceWindow;
import org.osc.core.broker.window.delete.DeleteWindowUtil;
import org.osc.core.broker.window.update.UpdateDistributedApplianceWindow;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CustomTable;
import com.vaadin.ui.CustomTable.ColumnGenerator;
import com.vaadin.ui.Notification;

@SuppressWarnings("serial")
@Component(service={DistributedApplianceView.class}, scope=ServiceScope.PROTOTYPE)
public class DistributedApplianceView extends CRUDBaseView<DistributedApplianceDto, VirtualSystemDto> {

    private static final String DA_HELP_GUID = "GUID-3FB92C5B-7F20-4B6A-B368-CA37C3E67007.html";
    private static final Logger log = LoggerFactory.getLogger(DistributedApplianceView.class);

    // objects of all the sub views this View supports
    private DeploymentSpecSubView dsSubView = null;
    private SecurityGroupInterfaceSubView sgiSubView = null;

    @Reference
    private AddDistributedApplianceServiceApi addDistributedApplianceService;

    @Reference
    private UpdateDistributedApplianceServiceApi updateDistributedApplianceService;

    @Reference
    private DeleteDistributedApplianceServiceApi deleteDistributedApplianceService;

    @Reference
    private ListDistributedApplianceServiceApi listDistributedApplianceService;

    @Reference
    private AddDeploymentSpecServiceApi addDeploymentSpecService;

    @Reference
    private UpdateDeploymentSpecServiceApi updateDeploymentSpecService;

    @Reference
    private DeleteDeploymentSpecServiceApi deleteDeploymentSpecService;

    @Reference
    private ListDeploymentSpecServiceByVirtualSystemApi listDeploymentSpecServiceByVirtualSystem;

    @Reference
    private SyncDeploymentSpecServiceApi syncDeploymentSpecService;

    @Reference
    private ForceDeleteVirtualSystemServiceApi forceDeleteVirtualSystemService;

    @Reference
    private ListApplianceModelSwVersionComboServiceApi listApplianceModelSwVersionComboService;

    @Reference
    private ListDomainsByMcIdServiceApi listDomainsByMcIdService;

    @Reference
    private ListEncapsulationTypeByVersionTypeAndModelApi listEncapsulationTypeByVersionTypeAndModel;

    @Reference
    private ListApplianceManagerConnectorServiceApi listApplianceManagerConnectorService;

    @Reference
    private ListAvailabilityZonesServiceApi listAvailabilityZonesService;

    @Reference
    private ListFloatingIpPoolsServiceApi listFloatingIpPoolsService;

    @Reference
    private ListHostServiceApi listHostService;

    @Reference
    private ListHostAggregateServiceApi listHostAggregateService;

    @Reference
    private ListNetworkServiceApi listNetworkService;

    @Reference
    private ListRegionServiceApi listRegionService;

    @Reference
    private ListProjectServiceApi listProjectService;

    @Reference
    private SyncDistributedApplianceServiceApi syncDistributedApplianceService;

    @Reference
    private AddSecurityGroupInterfaceServiceApi addSecurityGroupInterfaceService;

    @Reference
    private DeleteSecurityGroupInterfaceServiceApi deleteSecurityGroupInterfaceService;

    @Reference
    private ListSecurityGroupInterfaceServiceByVirtualSystemApi listSecurityGroupInterfaceServiceByVirtualSystem;

    @Reference
    private ListVirtualSystemPolicyServiceApi listVirtualSystemPolicyService;

    @Reference
    private UpdateSecurityGroupInterfaceServiceApi updateSecurityGroupInterfaceService;

    @Reference
    private ListVirtualizationConnectorBySwVersionServiceApi listVirtualizationConnectorBySwVersionService;

    @Reference
    private ValidationApi validator;

    @Reference
    private GetDtoFromEntityServiceFactoryApi getDtoFromEntityServiceFactory;

    @Reference
    private ServerApi server;

    @Activate
    private void activate() {

        Map<String, CRUDBaseSubView<?, ?>> childSubViewMap = new HashMap<String, CRUDBaseSubView<?, ?>>();
        childSubViewMap.put("Deployment Specification View", this.dsSubView);
        childSubViewMap.put("Traffic to Policy Mapping View", this.sgiSubView);

        List<ToolbarButtons> childToolbar = new ArrayList<>();
        childToolbar.add(ToolbarButtons.DEPLOYMENTS);
        childToolbar.add(ToolbarButtons.SECURITY_GROUP_INTERFACES);
        childToolbar.add(ToolbarButtons.DELETE_CHILD);

        createView("Distributed Appliances",
                Arrays.asList(ToolbarButtons.ADD, ToolbarButtons.EDIT, ToolbarButtons.DELETE, ToolbarButtons.CONFORM),
                false, "Virtual Systems", childToolbar, null, childSubViewMap);
    }

    @Override
    public void buttonClicked(ClickEvent event) throws Exception {
        if (event.getButton().getId().equals(ToolbarButtons.ADD.getId())) {
            log.info("Redirecting to Add Distributed Appliance Window");
            ViewUtil.addWindow(new AddDistributedApplianceWindow(this,
                    this.addDistributedApplianceService, this.listApplianceModelSwVersionComboService,
                    this.listDomainsByMcIdService, this.listEncapsulationTypeByVersionTypeAndModel,
                    this.listApplianceManagerConnectorService,
                    this.listVirtualizationConnectorBySwVersionService,
                    this.validator, this.server));
        }
        if (event.getButton().getId().equals(ToolbarButtons.EDIT.getId())) {
            log.info("Redirecting to Update Appliance Window");
            boolean markForDeletion = (boolean) getParentContainer()
                    .getContainerProperty(getParentItemId(), "markForDeletion").getValue();
            if (markForDeletion) {
                ViewUtil.iscNotification("Modification of deleted distributed appliance is not allowed.",
                        Notification.Type.WARNING_MESSAGE);

            } else {
                ViewUtil.addWindow(new UpdateDistributedApplianceWindow(this, this.updateDistributedApplianceService,
                        this.listApplianceModelSwVersionComboService, this.listDomainsByMcIdService,
                        this.listEncapsulationTypeByVersionTypeAndModel, this.listApplianceManagerConnectorService,
                        this.listVirtualizationConnectorBySwVersionService,
                        this.validator, this.server));
            }
        }
        if (event.getButton().getId().equals(ToolbarButtons.DELETE.getId())) {
            log.info("Redirecting to Delete Distributed Appliance Window");
            DeleteWindowUtil.deleteDistributedAppliance(getParentItem().getBean(), this.deleteDistributedApplianceService, this.server);
        }
        if (event.getButton().getId().equals(ToolbarButtons.CONFORM.getId())) {
            conformDistributedAppliace(getParentItemId());
        }
        if (event.getButton().getId().equals(ToolbarButtons.DEPLOYMENTS.getId())) {
            switchToDsSubView();
        }

        if (event.getButton().getId().equals(ToolbarButtons.SECURITY_GROUP_INTERFACES.getId())) {
            switchToSgiSubView();
        }

        if (event.getButton().getId().equals(ToolbarButtons.DELETE_CHILD.getId())) {
            DeleteWindowUtil.deleteVirtualSystem(this.forceDeleteVirtualSystemService, getChildItem().getBean(), this.server);
        }
    }

    private void switchToDsSubView() {
        VirtualSystemDto dto = this.childContainer.getItem(getChildItemId()).getBean();
        this.dsSubView = new DeploymentSpecSubView(
                "Deployment Specifications for Virtual System - " + dto.getName() + " (Virtual Connector: '"
                        + dto.getVirtualizationConnectorName() + "')",
                        new ToolbarButtons[] { ToolbarButtons.BACK, ToolbarButtons.ADD, ToolbarButtons.EDIT,
                                ToolbarButtons.DELETE, ToolbarButtons.CONFORM },
                        this, this.childContainer.getItem(getChildItemId()).getBean(),
                        this.addDeploymentSpecService, this.updateDeploymentSpecService,
                        this.deleteDeploymentSpecService, this.listDeploymentSpecServiceByVirtualSystem,
                        this.syncDeploymentSpecService, this.listAvailabilityZonesService,
                        this.listFloatingIpPoolsService, this.listHostService,
                        this.listHostAggregateService, this.listNetworkService,
                        this.listRegionService, this.listProjectService, this.server);

        // Replacing childSubView map entry with the newly instantiated class on the same key
        // Required to receive delegated broadcasted messages
        this.childSubViewMap.put(getKeyforChildSubView(1), this.dsSubView);
        this.viewSplitter.removeComponent(this.childContainerLayout);
        this.viewSplitter.addComponent(this.dsSubView);
    }

    private void switchToSgiSubView() throws Exception {
        this.sgiSubView = new SecurityGroupInterfaceSubView(
                "Policy Mappings for Virtual System - "
                        + this.childContainer.getItem(getChildItemId()).getBean().getName(),
                        new ToolbarButtons[] { ToolbarButtons.BACK, ToolbarButtons.ADD, ToolbarButtons.EDIT,
                                ToolbarButtons.DELETE },
                        this, this.childContainer.getItem(getChildItemId()).getBean(),
                        this.addSecurityGroupInterfaceService, this.deleteSecurityGroupInterfaceService,
                        this.listSecurityGroupInterfaceServiceByVirtualSystem, this.listVirtualSystemPolicyService,
                        this.updateSecurityGroupInterfaceService,
                        this.getDtoFromEntityServiceFactory, this.server);
        // Replacing childSubView map entry with the newly instantiated class on the same key
        // Required to receive delegated broadcasted messages
        this.childSubViewMap.put(getKeyforChildSubView(2), this.sgiSubView);
        this.viewSplitter.removeComponent(this.childContainerLayout);
        this.viewSplitter.addComponent(this.sgiSubView);
    }

    public void conformDistributedAppliace(Long daId) {
        log.info("Syncing DA " + daId.toString());
        BaseIdRequest request = new BaseIdRequest(daId);
        BaseJobResponse response = new BaseJobResponse();

        try {
            response = this.syncDistributedApplianceService.dispatch(request);
            ViewUtil.showJobNotification(response.getJobId(), this.server);
        } catch (Exception e) {
            log.error("Error!", e);
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }

    @Override
    public void initParentTable() {
        this.parentContainer = new BeanContainer<Long, DistributedApplianceDto>(DistributedApplianceDto.class);
        this.parentTable.setContainerDataSource(this.parentContainer);
        this.parentTable.setVisibleColumns("name", "applianceManagerConnectorName", "applianceModel", "applianceSoftwareVersionName",
                "lastJobStatus", "markForDeletion");

        this.parentTable.addGeneratedColumn("lastJobStatus", new ColumnGenerator() {
            @Override
            public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                DistributedApplianceDto distributedApplianceDto = DistributedApplianceView.this.parentContainer
                        .getItem(itemId).getBean();
                return ViewUtil.generateJobLink(distributedApplianceDto.getLastJobStatus(),
                        distributedApplianceDto.getLastJobState(), distributedApplianceDto.getLastJobId(), DistributedApplianceView.this.server);
            }
        });

        this.parentTable.addGeneratedColumn("applianceManagerConnectorName", new ColumnGenerator() {
            @Override
            public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                DistributedApplianceDto distributedApplianceDto = DistributedApplianceView.this.parentContainer
                        .getItem(itemId).getBean();
                return ViewUtil.generateObjectLink(new LockObjectDto(distributedApplianceDto.getMcId(),
                        distributedApplianceDto.getApplianceManagerConnectorName(),
                        ObjectTypeDto.APPLIANCE_MANAGER_CONNECTOR), DistributedApplianceView.this.server);
            }
        });

        // renaming column header
        this.parentTable.setColumnHeader("name", "Name");
        this.parentTable.setColumnHeader("applianceManagerConnectorName", "Manager");
        this.parentTable.setColumnHeader("applianceModel", "Model");
        this.parentTable.setColumnHeader("applianceSoftwareVersionName", "Version");
        this.parentTable.setColumnHeader("lastJobStatus", "Last Job Status");
        this.parentTable.setColumnHeader("markForDeletion", "Deleted");
    }

    @Override
    public void populateParentTable() {

        ListResponse<DistributedApplianceDto> res;
        try {
            res = this.listDistributedApplianceService.dispatch(new BaseRequest<>());
            List<DistributedApplianceDto> listResponse = res.getList();
            this.parentContainer.removeAllItems();
            // creating table with list of vendors
            for (DistributedApplianceDto da : listResponse) {
                this.parentContainer.addItem(da.getId(), da);
            }

        } catch (Exception e) {
            log.error("Fail to populate Distributed Appliance table", e);
            ViewUtil.iscNotification("Fail to populate Distributed Appliance table (" + e.getMessage() + ")",
                    Notification.Type.ERROR_MESSAGE);
        }

    }

    @Override
    public void initChildTable() {
        this.childContainer = new BeanContainer<Long, VirtualSystemDto>(VirtualSystemDto.class);
        this.childTable.setContainerDataSource(this.childContainer);
        // Creating table
        this.childTable.setVisibleColumns("name", "virtualizationConnectorName", "virtualizationType", "domainName",
                "markForDeletion");

        this.childTable.addGeneratedColumn("virtualizationConnectorName", new ColumnGenerator() {
            @Override
            public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                VirtualSystemDto vsDto = DistributedApplianceView.this.childContainer.getItem(itemId).getBean();
                return ViewUtil.generateObjectLink(
                        new LockObjectDto(vsDto.getVcId(), vsDto.getVirtualizationConnectorName(),
                                ObjectTypeDto.VIRTUALIZATION_CONNECTOR), DistributedApplianceView.this.server);
            }
        });

        // re-naming table header columns
        this.childTable.setColumnHeader("name", "VSS Name");
        this.childTable.setColumnHeader("virtualizationConnectorName", "Virtualization Connector");
        this.childTable.setColumnHeader("virtualizationType", "Virtualization Type");
        this.childTable.setColumnHeader("domainName", "Domain");
        this.childTable.setColumnHeader("markForDeletion", "Deleted");
    }

    @Override
    public void populateChildTable(BeanItem<DistributedApplianceDto> parentItem) {
        if (parentItem != null) {
            try {
                DistributedApplianceDto da = parentItem.getBean();
                // creating table with list of vendors
                this.childContainer.removeAllItems();
                for (VirtualSystemDto aVersion : da.getVirtualizationSystems()) {
                    this.childContainer.addItem(aVersion.getId(), aVersion);
                }
            } catch (Exception e) {
                log.error("Fail to populate Virtual System table", e);
                ViewUtil.iscNotification("Fail to populate Virtual System table (" + e.getMessage() + ")",
                        Notification.Type.ERROR_MESSAGE);
            }
        } else {
            this.childContainer.removeAllItems();
            ViewUtil.setButtonsEnabled(false, this.childToolbar);
        }
    }

    @Override
    protected void childTableClicked(long childItemId) {
        super.childTableClicked(childItemId);
        if (childItemId != NULL_SELECTION_ITEM_ID) {
            VirtualSystemDto selectedVs = this.childContainer.getItem(childItemId).getBean();

            ViewUtil.enableToolBarButtons(selectedVs.isMarkForDeletion(), this.childToolbar,
                    Arrays.asList(ToolbarButtons.DELETE_CHILD.getId()));

            if (selectedVs.getVirtualizationType().isOpenstack()) {
                // Enable SubView Buttons if VS is type OPENSTACK
                ViewUtil.enableToolBarButtons(true, this.childToolbar,
                        Arrays.asList(ToolbarButtons.DEPLOYMENTS.getId()));
            }
        }

    }

    @Override
    protected String getParentHelpGuid() {
        return DA_HELP_GUID;
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        super.enter(event);
        String parameters = event.getParameters();
        if (!StringUtils.isEmpty(parameters)) {
            Map<String, String> paramMap = ViewUtil.stringToMap(parameters);
            try {
                String da = paramMap.get(ViewUtil.DA_ID_PARAM_KEY);
                String vs = paramMap.get(ViewUtil.VS_ID_PARAM_KEY);
                String ds = paramMap.get(ViewUtil.DS_ID_PARAM_KEY);
                String sgi = paramMap.get(ViewUtil.SGI_ID_PARAM_KEY);

                Long daId = null;
                if (da != null) {
                    daId = Long.parseLong(da);
                }
                Long vsId = null;
                if (vs != null) {
                    vsId = Long.parseLong(vs);
                }
                Long dsId = null;
                if (ds != null) {
                    dsId = Long.parseLong(ds);
                }
                Long sgiId = null;
                if (sgi != null) {
                    sgiId = Long.parseLong(sgi);
                }

                if (dsId != null) {
                    GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
                    getDtoRequest.setEntityId(dsId);
                    getDtoRequest.setEntityName("DeploymentSpec");
                    GetDtoFromEntityServiceApi<DeploymentSpecDto> getService = this.getDtoFromEntityServiceFactory.getService(DeploymentSpecDto.class);
                    BaseDtoResponse<DeploymentSpecDto> dsDto;
                    try {
                        dsDto = getService.dispatch(getDtoRequest);
                        vsId = dsDto.getDto().getParentId();
                    } catch (Exception e) {
                        log.error("Error while getting ds " + dsId + ".");
                    }
                }

                if (sgiId != null) {
                    GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
                    getDtoRequest.setEntityId(sgiId);
                    getDtoRequest.setEntityName("SecurityGroupInterface");
                    GetDtoFromEntityServiceApi<SecurityGroupInterfaceDto> getService = this.getDtoFromEntityServiceFactory.getService(SecurityGroupInterfaceDto.class);
                    BaseDtoResponse<SecurityGroupInterfaceDto> sgiDto;
                    try {
                        sgiDto = getService.dispatch(getDtoRequest);
                        vsId = sgiDto.getDto().getParentId();
                    } catch (Exception e) {
                        log.error("Error while getting ds " + sgiId + ".");
                    }
                }

                if (vsId != null) {
                    GetDtoFromEntityRequest getDtoRequest = new GetDtoFromEntityRequest();
                    getDtoRequest.setEntityId(vsId);
                    getDtoRequest.setEntityName("VirtualSystem");
                    GetDtoFromEntityServiceApi<VirtualSystemDto> getVsSvc = this.getDtoFromEntityServiceFactory.getService(VirtualSystemDto.class);

                    BaseDtoResponse<VirtualSystemDto> vsDto;
                    try {
                        vsDto = getVsSvc.dispatch(getDtoRequest);
                        daId = vsDto.getDto().getParentId();
                    } catch (Exception e) {
                        log.error("Error while getting vs " + vsId + ".");
                    }
                }

                if (daId != null) {
                    log.info("Entered DA View with Id: " + daId);
                    this.parentTable.select(daId);
                    this.parentTable.setCurrentPageFirstItemIndex(getParentContainer().indexOfId(daId));
                }
                if (vsId != null) {
                    log.info("Entered VS View with Id: " + vsId);
                    this.childTable.select(vsId);
                    this.childTable.setCurrentPageFirstItemIndex(getParentContainer().indexOfId(vsId));
                }
                if (dsId != null) {
                    log.info("Entered DS View with Id: " + dsId);
                    switchToDsSubView();
                    this.dsSubView.table.select(dsId);
                    this.dsSubView.table.setCurrentPageFirstItemIndex(getParentContainer().indexOfId(dsId));
                }
                if (sgiId != null) {
                    log.info("Entered SGI View with Id: " + sgiId);
                    switchToSgiSubView();
                    this.sgiSubView.table.select(sgiId);
                    this.sgiSubView.table.setCurrentPageFirstItemIndex(getParentContainer().indexOfId(sgiId));
                }

            } catch (Exception ex) {
                log.error("Invalid Parameters for DA View. " + parameters, ex);
            }
        }
    }

}
