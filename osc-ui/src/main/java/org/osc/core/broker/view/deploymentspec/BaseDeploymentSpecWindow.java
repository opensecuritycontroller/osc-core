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

import java.util.ArrayList;
import java.util.List;

import org.osc.core.broker.service.api.ListAvailabilityZonesServiceApi;
import org.osc.core.broker.service.api.ListFloatingIpPoolsServiceApi;
import org.osc.core.broker.service.api.ListHostAggregateServiceApi;
import org.osc.core.broker.service.api.ListHostServiceApi;
import org.osc.core.broker.service.api.ListNetworkServiceApi;
import org.osc.core.broker.service.api.ListProjectServiceApi;
import org.osc.core.broker.service.api.ListRegionServiceApi;
import org.osc.core.broker.service.dto.openstack.AvailabilityZoneDto;
import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.dto.openstack.HostAggregateDto;
import org.osc.core.broker.service.dto.openstack.HostDto;
import org.osc.core.broker.service.dto.openstack.OsNetworkDto;
import org.osc.core.broker.service.dto.openstack.OsProjectDto;
import org.osc.core.broker.service.exceptions.ExtensionNotPresentException;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.request.BaseOpenStackRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.view.common.StyleConstants;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.LoadingIndicatorCRUDBaseWindow;
import org.osc.core.broker.window.ProgressIndicatorWindow;
import org.osc.core.ui.LogProvider;
import org.slf4j.Logger;
import org.vaadin.risto.stepper.IntStepper;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Notification;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;

public abstract class BaseDeploymentSpecWindow extends LoadingIndicatorCRUDBaseWindow {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private static Logger log = LogProvider.getLogger(BaseDeploymentSpecWindow.class);

    protected static final String AVAILABILITY_ZONES = "By Availability Zone";
    protected static final String HOSTS = "By Host";
    protected static final String HOST_AGGREGATES = "By Host Aggregates";
    protected static final String NONE = "All (Hosts in selected Region)";

    protected DeploymentSpecDto deploymentSpecDto;

    private ValueChangeListener projectChangedListener;
    private ValueChangeListener regionChangedListener;

    protected TextField name;
    protected IntStepper count;
    protected CheckBox shared;
    protected ComboBox region;
    protected ComboBox floatingIpPool;
    protected ComboBox project;
    protected OptionGroup userOption;
    protected Table optionTable;
    protected ComboBox managementNetwork;
    protected ComboBox inspectionNetwork;
    protected Long vsId;
    protected Panel optionPanel;

    private final ListAvailabilityZonesServiceApi listAvailabilityZonesService;

    private final ListFloatingIpPoolsServiceApi listFloatingIpPoolsService;

    private final ListHostAggregateServiceApi listHostAggregateService;

    private final ListHostServiceApi listHostService;

    private final ListNetworkServiceApi listNetworkService;

    private final ListRegionServiceApi listRegionService;

    private final ListProjectServiceApi listProjectService;

    public BaseDeploymentSpecWindow(DeploymentSpecDto deploymentSpecDto,
            ListAvailabilityZonesServiceApi listAvailabilityZonesService,
            ListFloatingIpPoolsServiceApi listFloatingIpPoolsService,
            ListHostServiceApi listHostService,
            ListHostAggregateServiceApi listHostAggregateService,
            ListNetworkServiceApi listNetworkService,
            ListRegionServiceApi listRegionService,
            ListProjectServiceApi listProjectService) {
        this.deploymentSpecDto = deploymentSpecDto;
        this.listAvailabilityZonesService = listAvailabilityZonesService;
        this.listFloatingIpPoolsService = listFloatingIpPoolsService;
        this.listNetworkService = listNetworkService;
        this.listRegionService = listRegionService;
        this.listProjectService = listProjectService;
        this.listHostService = listHostService;
        this.listHostAggregateService = listHostAggregateService;
        this.vsId = deploymentSpecDto.getParentId();
        initListeners();
    }

    @Override
    public void initForm() {
        this.form.addComponent(getName());
        this.form.addComponent(getProjects());
        this.form.addComponent(getRegion());
        this.form.addComponent(getUserOptions());
        this.form.addComponent(getOptionTable());

        List<ComboBox> networks = getNetworkComboBoxes();
        for (ComboBox combobox : networks) {
            this.form.addComponent(combobox);
        }
        this.form.addComponent(getIPPool());
        this.form.addComponent(getCount());
        this.form.addComponent(getSharedCheckBox());
        this.name.focus();
        getCount().setEnabled(false);
    }

    @Override
    public void makeServiceCalls(ProgressIndicatorWindow progressIndicatorWindow) {
        progressIndicatorWindow.updateStatus("Populating Project Information");
        // Dont auto select project in case of update, since update sets the project automatically once the load completes.
        populateProjects(!isUpdateWindow());
    }

    protected TextField getName() {
        this.name = new TextField("Name");
        this.name.setImmediate(true);
        this.name.setRequired(true);
        this.name.setRequiredError("Name cannot be empty");
        return this.name;
    }

    protected Component getProjects() {
        try {
            this.project = new ComboBox("Select Project");
            this.project.setTextInputAllowed(true);
            this.project.setNullSelectionAllowed(false);
            this.project.setImmediate(true);
            this.project.setRequired(true);
            this.project.setRequiredError("Project cannot be empty");

        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
            log.error("Error populating Project List combobox", e);
        }

        return this.project;
    }

    @SuppressWarnings("serial")
    protected OptionGroup getUserOptions() {
        this.userOption = new OptionGroup("Selection Criterion:");
        this.userOption.addItem(NONE);
        this.userOption.addItem(AVAILABILITY_ZONES);
        this.userOption.addItem(HOST_AGGREGATES);
        this.userOption.addItem(HOSTS);
        this.userOption.select(NONE);
        this.userOption.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                populateOptionTable();
            }
        });

        return this.userOption;
    }

    protected ComboBox getRegion() {
        this.region = new ComboBox("Select Region");
        this.region.setTextInputAllowed(false);
        this.region.setNullSelectionAllowed(false);
        this.region.setImmediate(true);
        this.region.setRequired(true);
        this.region.setRequiredError("Region cannot be empty");

        return this.region;
    }

    protected ComboBox getIPPool() {
        this.floatingIpPool = new ComboBox("Select Floating IP Pool");
        this.floatingIpPool.setTextInputAllowed(false);
        this.floatingIpPool.setNullSelectionAllowed(true);
        this.floatingIpPool.setImmediate(true);
        this.floatingIpPool.setRequired(false);
        return this.floatingIpPool;
    }

    protected List<ComboBox> getNetworkComboBoxes() {

        List<ComboBox> networkComboBox = new ArrayList<>();
        this.managementNetwork = new ComboBox("Select Management Network");
        this.managementNetwork.setTextInputAllowed(false);
        this.managementNetwork.setNullSelectionAllowed(false);
        this.managementNetwork.setImmediate(true);
        this.managementNetwork.setRequired(true);
        this.managementNetwork.setRequiredError("Management Network cannot be empty");

        networkComboBox.add(this.managementNetwork);

        this.inspectionNetwork = new ComboBox("Select Inspection Network");
        this.inspectionNetwork.setTextInputAllowed(false);
        this.inspectionNetwork.setNullSelectionAllowed(false);
        this.inspectionNetwork.setImmediate(true);
        this.inspectionNetwork.setRequired(true);
        this.inspectionNetwork.setRequiredError("Inspection Network cannot be empty");

        networkComboBox.add(this.inspectionNetwork);

        return networkComboBox;

    }

    private void populateNetworks(ComboBox networkComboBox, List<OsNetworkDto> networkList) {
        try {
            networkComboBox.removeAllItems();
            if (networkList != null) {
                // Calling List Network Service
                BeanItemContainer<OsNetworkDto> networkListContainer = new BeanItemContainer<>(OsNetworkDto.class,
                        networkList);

                networkComboBox.setContainerDataSource(networkListContainer);
                networkComboBox.setItemCaptionPropertyId("name");
                if (networkList.size() > 0) {
                    networkComboBox.select(networkListContainer.getIdByIndex(0));
                }
            }
        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
            log.error("Error getting Network List", e);
        }
    }

    private List<OsNetworkDto> getNetworks() {
        try {
            OsProjectDto selectedProject = (OsProjectDto) this.project.getValue();
            if (selectedProject != null && this.region.getValue() != null) {
                // Calling List Network Service
                BaseOpenStackRequest req = new BaseOpenStackRequest();
                req.setId(this.vsId);
                req.setRegion((String) this.region.getValue());
                req.setProjectName(selectedProject.getName());
                req.setProjectId(selectedProject.getId());

                List<OsNetworkDto> res = this.listNetworkService.dispatch(req).getList();

                return res;
            }
        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
            log.error("Error getting Network List", e);
        }
        return null;
    }

    @SuppressWarnings("serial")
    protected Panel getOptionTable() {
        this.optionTable = new Table();
        this.optionTable.setPageLength(3);
        this.optionTable.setSizeFull();
        this.optionTable.setImmediate(true);
        this.optionTable.addGeneratedColumn("Enabled", new CheckBoxGenerator());
        this.optionTable.addContainerProperty("Name", String.class, null);
        this.optionTable.addItemClickListener(new ItemClickListener() {
            @Override
            public void itemClick(ItemClickEvent event) {
                optionTableClicked(event);
            }
        });

        this.optionPanel = new Panel();
        this.optionPanel.addStyleName(StyleConstants.FORM_PANEL);
        this.optionPanel.setWidth(100, Sizeable.Unit.PERCENTAGE);
        this.optionPanel.setContent(this.optionTable);

        return this.optionPanel;

    }

    protected IntStepper getCount() {
        if (this.count == null) {
            this.count = new IntStepper("Deployment Count");
            this.count.setImmediate(true);
            this.count.setValue(1);
            this.count.setStepAmount(1);
            this.count.setMinValue(1);
            this.count.setRequiredError("Instance Count cannot be empty");
        }
        return this.count;
    }

    protected CheckBox getSharedCheckBox() {
        this.shared = new CheckBox("Shared");
        this.shared.setValue(true);
        this.shared.setImmediate(true);

        return this.shared;
    }

    private void populateProjects(boolean autoSelect) {
        try {
            // Calling List Service
            BaseIdRequest req = new BaseIdRequest();
            req.setId(this.vsId);

            List<OsProjectDto> projectList = this.listProjectService.dispatch(req).getList();

            this.project.removeValueChangeListener(this.projectChangedListener);
            this.project.removeAllItems();

            BeanItemContainer<OsProjectDto> projectListContainer = new BeanItemContainer<>(OsProjectDto.class, projectList);
            this.project.setContainerDataSource(projectListContainer);
            this.project.setItemCaptionPropertyId("name");

            this.project.addValueChangeListener(this.projectChangedListener);

            if (autoSelect && projectListContainer.size() > 0) {
                this.project.select(projectListContainer.getIdByIndex(0));
            }
        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
            log.error("Error getting project List", e);
        }

    }

    private void populateRegion() {
        try {
            OsProjectDto projectDto = (OsProjectDto) this.project.getValue();

            if (projectDto != null) {
                this.region.removeValueChangeListener(this.regionChangedListener);
                this.region.removeAllItems();

                BaseOpenStackRequest req = new BaseOpenStackRequest();
                req.setProjectName(projectDto.getName());
                req.setId(this.vsId);
                ListResponse<String> response = this.listRegionService.dispatch(req);

                this.region.addItems(response.getList());

                this.region.addValueChangeListener(this.regionChangedListener);

                if (response.getList().get(0) != null) {
                    this.region.select(response.getList().get(0));
                }
            } else {
                this.region.removeAllItems();
            }
        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
            log.error("Error getting Region List", e);
        }
    }

    private void populateOptionTable() {

        try {
            this.form.replaceComponent(this.optionPanel, getOptionTable());

            OsProjectDto selectedProject = (OsProjectDto) this.project.getValue();
            if (selectedProject != null && this.region.getValue() != null) {
                BaseOpenStackRequest req = new BaseOpenStackRequest();
                req.setId(this.vsId);
                req.setProjectName(selectedProject.getName());
                req.setProjectId(selectedProject.getId());
                req.setRegion((String) this.region.getValue());

                // creating Option Table
                this.optionTable.addContainerProperty("Enabled", Boolean.class, false);
                // remove previous columns
                this.optionTable.removeAllItems();

                if (this.userOption.getValue() == AVAILABILITY_ZONES) {
                    getCount().setValue(1);
                    getCount().setEnabled(false);

                    ListResponse<AvailabilityZoneDto> res = this.listAvailabilityZonesService.dispatch(req);
                    for (AvailabilityZoneDto az : res.getList()) {
                        this.optionTable.addItem(new Object[] { az.getZone() }, az);
                    }
                } else if (this.userOption.getValue() == HOSTS) {
                    getCount().setEnabled(true);

                    ListResponse<HostDto> res = this.listHostService.dispatch(req);
                    for (HostDto host : res.getList()) {
                        this.optionTable.addItem(new Object[] { host.getName() }, host);
                    }
                } else if (this.userOption.getValue() == HOST_AGGREGATES) {
                    getCount().setValue(1);
                    getCount().setEnabled(false);

                    ListResponse<HostAggregateDto> res = this.listHostAggregateService.dispatch(req);
                    for (HostAggregateDto hostAggr : res.getList()) {
                        this.optionTable.addItem(new Object[] { hostAggr.getName() }, hostAggr);
                    }
                } else {
                    getCount().setValue(1);
                    getCount().setEnabled(false);
                }
                this.optionTable.sort(new Object[] { "Name" }, new boolean[] { true });

            } else {
                this.optionTable.removeAllItems();
            }

        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
            log.error("Failed to get Option Table", e);
        }

    }

    private void populateFloatingPool() {
        this.floatingIpPool.removeAllItems();
        try {
            OsProjectDto selectedProject = (OsProjectDto) this.project.getValue();
            if (selectedProject != null && this.region.getValue() != null) {
                BaseOpenStackRequest req = new BaseOpenStackRequest();
                req.setId(this.vsId);
                req.setProjectName(selectedProject.getName());
                req.setProjectId(selectedProject.getId());
                req.setRegion((String) this.region.getValue());

                List<String> floatingIpPoolList = this.listFloatingIpPoolsService.dispatch(req).getList();

                if (floatingIpPoolList.size() > 0) {
                    this.floatingIpPool.addItems(floatingIpPoolList);
                }
            }
        } catch (ExtensionNotPresentException notPresentException) {
            ViewUtil.iscNotification(notPresentException.getMessage(), Notification.Type.WARNING_MESSAGE);
            log.warn("Failed to get IP Pool", notPresentException);
        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
            log.error("Failed to get IP Pool", e);
        }

    }

    @Override
    public boolean validateForm() {
        this.name.validate();
        // UI validation empty Selection while using AZ/Host deployment mode
        if (this.userOption.getValue().equals(AVAILABILITY_ZONES) || this.userOption.getValue().equals(HOSTS)
        		|| this.userOption.getValue().equals(HOST_AGGREGATES)) {
            int count = 0;
            for (Object id : this.optionTable.getItemIds()) {
                if (this.optionTable.getContainerProperty(id, "Enabled").getValue().equals(true)) {
                    count++;
                }
            }
            if (count == 0) {
                ViewUtil.iscNotification("Atleast one selection is Required!", Notification.Type.ERROR_MESSAGE);
                return false;
            }
        }

        this.project.validate();
        this.region.validate();
        this.managementNetwork.validate();
        this.inspectionNetwork.validate();
        this.floatingIpPool.validate();
        return true;

    }

    @SuppressWarnings("serial")
    private class CheckBoxGenerator implements Table.ColumnGenerator {
        @Override
        public Object generateCell(Table source, Object itemId, Object columnId) {
            Property<?> prop = source.getItem(itemId).getItemProperty(columnId);
            return new CheckBox(null, prop);
        }
    }

    @SuppressWarnings("unchecked")
    private void optionTableClicked(ItemClickEvent event) {
        if (this.userOption.getValue() == NONE) {
            return;
        }
        Object itemId = 0L;
        if (this.userOption.getValue() == AVAILABILITY_ZONES) {
            itemId = event.getItemId();
        } else if (this.userOption.getValue() == HOSTS) {
            itemId = event.getItemId();
        } else if (this.userOption.getValue() == HOST_AGGREGATES) {
            itemId = event.getItemId();
        }
        if (this.optionTable.getContainerProperty(itemId, "Enabled").getValue().equals(true)) {
            this.optionTable.getContainerProperty(itemId, "Enabled").setValue(false);
        } else {
            this.optionTable.getContainerProperty(itemId, "Enabled").setValue(true);
        }
    }

    @SuppressWarnings("serial")
    private void initListeners() {
        this.projectChangedListener = new ValueChangeListener() {

            @Override
            public void valueChange(ValueChangeEvent event) {
                if (BaseDeploymentSpecWindow.this.region != null) {
                    populateRegion();
                }
                if (BaseDeploymentSpecWindow.this.managementNetwork != null
                        && BaseDeploymentSpecWindow.this.inspectionNetwork != null) {
                    List<OsNetworkDto> networks = getNetworks();
                    populateNetworks(BaseDeploymentSpecWindow.this.managementNetwork, networks);
                    populateNetworks(BaseDeploymentSpecWindow.this.inspectionNetwork, networks);
                }
                if (BaseDeploymentSpecWindow.this.floatingIpPool != null) {
                    populateFloatingPool();
                }
            }
        };
        this.regionChangedListener = new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                if (BaseDeploymentSpecWindow.this.optionTable != null) {
                    populateOptionTable();
                }
            }
        };
    }

    private boolean isUpdateWindow() {
        return this.deploymentSpecDto.getId() != null;
    }
}
