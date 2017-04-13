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
package org.osc.core.broker.view.deploymentspec;

import java.util.Arrays;

import org.jboss.logging.Logger;
import org.osc.core.broker.service.ListDeploymentSpecServiceByVirtualSystem;
import org.osc.core.broker.service.SyncDeploymentSpecService;
import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.view.CRUDBaseSubView;
import org.osc.core.broker.view.CRUDBaseView;
import org.osc.core.broker.view.util.ToolbarButtons;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.delete.DeleteWindowUtil;

import com.vaadin.data.util.BeanContainer;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CustomTable;
import com.vaadin.ui.CustomTable.ColumnGenerator;
import com.vaadin.ui.Notification;

public class DeploymentSpecSubView extends CRUDBaseSubView<VirtualSystemDto, DeploymentSpecDto> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(DeploymentSpecSubView.class);
    private static final String DEPLOYMENT_SPEC_HELP_GUID = "GUID-53968F65-9F9C-4D75-996C-4B48185A5A4E.html";

    public DeploymentSpecSubView(String title, ToolbarButtons[] buttons, CRUDBaseView<?, ?> currentView,
            VirtualSystemDto parent) {
        super(currentView, title, buttons, parent);
        if (parent.isMarkForDeletion()) {
            //Disable CRUD buttons
            ViewUtil.setButtonsEnabled(false, this.toolbar,
                    Arrays.asList(ToolbarButtons.BACK.getId(), ToolbarButtons.DELETE.getId()));

            this.table.setEnabled(false);
        }
    }

    @SuppressWarnings("serial")
    @Override
    public void initTable() {
        this.tableContainer = new BeanContainer<Long, DeploymentSpecDto>(DeploymentSpecDto.class);
        this.table.setContainerDataSource(this.tableContainer);
        this.table.setVisibleColumns("name", "tenantName", "managementNetworkName", "markForDeletion",
                "lastJobStatus");

        // Customizing column header names
        this.table.setColumnHeader("name", "Name");
        this.table.setColumnHeader("tenantName", "Tenant");
        this.table.setColumnHeader("managementNetworkName", "Network");
        this.table.setColumnHeader("markForDeletion", "Deleted");
        this.table.addGeneratedColumn("lastJobStatus", new ColumnGenerator() {
            @Override
            public Object generateCell(CustomTable source, Object itemId, Object columnId) {
                DeploymentSpecDto dsDto = DeploymentSpecSubView.this.tableContainer.getItem(itemId).getBean();
                return ViewUtil.generateJobLink(dsDto.getLastJobStatus(), dsDto.getLastJobState(),
                        dsDto.getLastJobId());
            }

        });
        this.table.setColumnHeader("lastJobStatus", "Last Job Status");
    }

    @Override
    public void populateTable() {
        try {

            BaseIdRequest request = new BaseIdRequest();
            request.setId(getDtoInContext().getId());
            ListDeploymentSpecServiceByVirtualSystem service = new ListDeploymentSpecServiceByVirtualSystem();
            ListResponse<DeploymentSpecDto> res = service.dispatch(request);
            this.tableContainer.removeAllItems();
            if (res.getList().size() <= 0) {
                ViewUtil.setButtonsEnabled(false, this.toolbar,
                        Arrays.asList(ToolbarButtons.ADD.getId(), ToolbarButtons.BACK.getId()));
            } else {
                for (DeploymentSpecDto ds : res.getList()) {
                    this.tableContainer.addItem(ds.getId(), ds);
                }
                ViewUtil.setButtonsEnabled(true, this.toolbar);
            }
        } catch (Exception e) {
            log.error("Failed to populate Deployment Specification", e);
        }
    }

    @Override
    public void buttonClicked(ClickEvent event) throws Exception {
        if (event.getButton().getId().equals(ToolbarButtons.ADD.getId())) {
            log.debug("Redirecting to Add Deployment Spec Window");
            ViewUtil.addWindow(new AddDeploymentSpecWindow(getDtoInContext().getId()));
        }
        if (event.getButton().getId().equals(ToolbarButtons.EDIT.getId())) {
            log.debug("Redirecting to Update Deployment Spec Window");
            boolean markForDeletion = getTableContainer().getItem(getSelectedItemId()).getBean().isMarkForDeletion();
            if (markForDeletion) {
                ViewUtil.iscNotification("Modification of deleted Deployment Specification is not allowed.",
                        Notification.Type.WARNING_MESSAGE);
            } else {
                ViewUtil.addWindow(
                        new UpdateDeploymentSpecWindow(getTableContainer().getItem(getSelectedItemId()).getBean()));
            }
        }
        if (event.getButton().getId() == ToolbarButtons.DELETE.getId()) {
            log.debug("Redirecting to Delete Deployment Spec Window");
            DeleteWindowUtil.deleteDeploymentSpec(getTableContainer().getItem(getSelectedItemId()).getBean());
        }
        if (event.getButton().getId() == ToolbarButtons.CONFORM.getId()) {
            conformDeploymentSpec(getSelectedItemId());
        }
        if (event.getButton().getId() == ToolbarButtons.BACK.getId()) {
            // removing object from the sub view map so it can be garbage collected
            this.currentView.childSubViewMap.put(this.currentView.getKeyforChildSubView(1), null);
            this.currentView.viewSplitter.removeComponent(this.currentView.viewSplitter.getSecondComponent());
            this.currentView.viewSplitter.addComponent(this.currentView.childContainerLayout);
        }
    }

    private void conformDeploymentSpec(long dsId) {
        log.info("Syncing DS " + dsId);

        DeploymentSpecDto requestDto = new DeploymentSpecDto();
        requestDto.setId(dsId);

        BaseRequest<DeploymentSpecDto> req = new BaseRequest<DeploymentSpecDto>();
        req.setDto(requestDto);

        try {
            SyncDeploymentSpecService syncDsService = new SyncDeploymentSpecService();
            BaseJobResponse response = syncDsService.dispatch(req);

            ViewUtil.showJobNotification(response.getJobId());
        } catch (Exception e) {
            log.error("Error!", e);
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }

    @Override
    protected String getSubViewHelpGuid() {
        return DEPLOYMENT_SPEC_HELP_GUID;
    }
}
