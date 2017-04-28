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
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.service.api.AddApplianceManagerConnectorServiceApi;
import org.osc.core.broker.service.api.DeleteApplianceManagerConnectorServiceApi;
import org.osc.core.broker.service.api.ListApplianceManagerConnectorServiceApi;
import org.osc.core.broker.service.api.ListManagerConnectoryPolicyServiceApi;
import org.osc.core.broker.service.api.SyncManagerConnectorServiceApi;
import org.osc.core.broker.service.api.UpdateApplianceManagerConnectorServiceApi;
import org.osc.core.broker.service.dto.ApplianceManagerConnectorDto;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.dto.PolicyDto;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.request.BaseJobRequest;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.view.util.ToolbarButtons;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.add.AddManagerConnectorWindow;
import org.osc.core.broker.window.delete.DeleteManagerConnectorWindow;
import org.osc.core.broker.window.update.UpdateManagerConnectorWindow;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CustomTable;
import com.vaadin.ui.CustomTable.ColumnGenerator;
import com.vaadin.ui.Notification;

@Component(service={ManagerConnectorView.class}, scope=ServiceScope.PROTOTYPE)
public class ManagerConnectorView extends CRUDBaseView<ApplianceManagerConnectorDto, PolicyDto> {
    private static final String MC_HELP_GUID = "GUID-14BF1C4E-4729-437A-BF60-A53EED74009C.html";
    private static final String POLICY_HELP_GUID = "GUID-14BF1C4E-4729-437A-BF60-A53EED74009C.html";

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(ManagerConnectorView.class);

    @Reference
    private ApiFactoryService apiFactoryService;

    @Reference
    private AddApplianceManagerConnectorServiceApi addManagerConnectorService;

    @Reference
    private UpdateApplianceManagerConnectorServiceApi updateManagerConnectorService;

    @Reference
    private DeleteApplianceManagerConnectorServiceApi deleteManagerConnectorService;

    @Reference
    private SyncManagerConnectorServiceApi syncManagerConnectorService;

    @Reference
    private ListApplianceManagerConnectorServiceApi listApplianceManagerConnectorService;

    @Reference
    private ListManagerConnectoryPolicyServiceApi listManagerConnectoryPolicyService;

    @Activate
    private void activate() {

        createView("Manager Connector", Arrays.asList(ToolbarButtons.ADD, ToolbarButtons.EDIT,
                ToolbarButtons.DELETE, ToolbarButtons.CONFORM), "Policies", null);
    }

    @Override
    public void buttonClicked(ClickEvent event) throws Exception {
        if (event.getButton().getId().equals(ToolbarButtons.ADD.getId())) {
            log.debug("Redirecting to Add Manager Connector Window");
            ViewUtil.addWindow(new AddManagerConnectorWindow(this, this.addManagerConnectorService));
        }
        if (event.getButton().getId().equals(ToolbarButtons.EDIT.getId())) {
            log.debug("Redirecting to Update Manager Connector Window");
            ViewUtil.addWindow(new UpdateManagerConnectorWindow(this, this.updateManagerConnectorService));
        }
        if (event.getButton().getId().equals(ToolbarButtons.DELETE.getId())) {
            log.debug("Redirecting to Delete Manager Connector Window");
            ViewUtil.addWindow(new DeleteManagerConnectorWindow(this, this.deleteManagerConnectorService));
        }
        if (event.getButton().getId().equals(ToolbarButtons.CONFORM.getId())) {
            conformManagerConnector(getParentItemId());
        }
    }

    private void conformManagerConnector(Long mcId) {
        log.info("Syncing MC " + mcId.toString());
        try {
            BaseJobRequest request = new BaseJobRequest(mcId);
            BaseJobResponse response = this.syncManagerConnectorService.dispatch(request);
            ViewUtil.showJobNotification(response.getJobId());
        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }

    @SuppressWarnings("serial")
    @Override
    public void initParentTable() {
        this.parentContainer = new BeanContainer<Long, ApplianceManagerConnectorDto>(ApplianceManagerConnectorDto.class);
        this.parentTable.setContainerDataSource(this.parentContainer);
        this.parentTable.setVisibleColumns("name", "managerType", "ipAddress", "lastJobStatus");

        this.parentTable.addGeneratedColumn("lastJobStatus", new ColumnGenerator() {
            @Override
            public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                ApplianceManagerConnectorDto managerConnectorDto = ManagerConnectorView.this.parentContainer.getItem(
                        itemId).getBean();
                return ViewUtil.generateJobLink(managerConnectorDto.getLastJobStatus(),
                        managerConnectorDto.getLastJobState(), managerConnectorDto.getLastJobId());
            }
        });

        this.parentTable.addGeneratedColumn("ipAddress", new ColumnGenerator() {
            @Override
            public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                ApplianceManagerConnectorDto managerConnectorDto = ManagerConnectorView.this.parentContainer.getItem(
                        itemId).getBean();

                try {
                    return ViewUtil.generateMgrLink(
                            managerConnectorDto.getIpAddress(),
                            ManagerConnectorView.this.apiFactoryService.createApplianceManagerApi(
                                    ManagerType.fromText(managerConnectorDto.getManagerType()))
                                .getManagerUrl(managerConnectorDto.getIpAddress()));
                } catch (Exception e) {
                    return ViewUtil.generateMgrLink("http://", managerConnectorDto.getIpAddress(), "", "");
                }

            }
        });

        // Customizing column header names
        this.parentTable.setColumnHeader("name", "Name");
        this.parentTable.setColumnHeader("managerType", "Type");
        this.parentTable.setColumnHeader("ipAddress", "Host");
        this.parentTable.setColumnHeader("lastJobStatus", "Last Job Status");
    }

    @Override
    public void populateParentTable() {

        BaseRequest<BaseDto> listRequest = new BaseRequest<>();
        ListResponse<ApplianceManagerConnectorDto> res;
        try {
            res = this.listApplianceManagerConnectorService.dispatch(listRequest);
            List<ApplianceManagerConnectorDto> listResponse = res.getList();
            this.parentContainer.removeAllItems();
            // Creating table with list of vendors
            for (ApplianceManagerConnectorDto mcm : listResponse) {
                this.parentContainer.addItem(mcm.getId(), mcm);
            }

        } catch (Exception e) {
            log.error("Fail to populate Manager Connector table", e);
            ViewUtil.iscNotification("Fail to populate Manager Connector table (" + e.getMessage() + ")",
                    Notification.Type.ERROR_MESSAGE);
        }

    }

    @Override
    public void initChildTable() {
        this.childContainer = new BeanContainer<Long, PolicyDto>(PolicyDto.class);
        this.childTable.setContainerDataSource(this.childContainer);
        this.childTable.setVisibleColumns("policyName", "mgrDomainName");

        this.childTable.setColumnHeader("policyName", "Name");
        this.childTable.setColumnHeader("mgrDomainName", "Domain");
    }

    @Override
    public void populateChildTable(BeanItem<ApplianceManagerConnectorDto> parentItem) {
        this.childContainer.removeAllItems();
        if (parentItem != null) {
            try {
                BaseIdRequest listRequest = new BaseIdRequest();
                listRequest.setId(getParentItemId());
                ListResponse<PolicyDto> res = this.listManagerConnectoryPolicyService.dispatch(listRequest);

                for (PolicyDto policy : res.getList()) {
                    this.childContainer.addItem(policy.getId(), policy);
                }

            } catch (Exception e) {
                log.error("Fail to populate Policy Table", e);
                ViewUtil.iscNotification("Fail to populate Policy table (" + e.getMessage() + ")",
                        Notification.Type.ERROR_MESSAGE);
            }
        }
    }

    @Override
    protected String getParentHelpGuid() {
        return MC_HELP_GUID;
    }

    @Override
    protected String getChildHelpGuid() {
        return POLICY_HELP_GUID;
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        super.enter(event);
        String parameters = event.getParameters();
        if (!StringUtils.isEmpty(parameters)) {
            Map<String, String> paramMap = ViewUtil.stringToMap(parameters);
            try {
                Long mcId = Long.parseLong(paramMap.get(ViewUtil.MC_ID_PARAM_KEY));
                log.info("Entered MC View with Id:" + mcId);
                this.parentTable.select(mcId);
                this.parentTable.setCurrentPageFirstItemIndex(getParentContainer().indexOfId(mcId));
            } catch (NumberFormatException ne) {
                log.warn("Invalid Parameters for MC View. " + parameters);
            }
        }
    }

}
