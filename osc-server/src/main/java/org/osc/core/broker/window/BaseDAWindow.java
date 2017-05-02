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
package org.osc.core.broker.window;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.ListApplianceManagerConnectorServiceApi;
import org.osc.core.broker.service.api.ListApplianceModelSwVersionComboServiceApi;
import org.osc.core.broker.service.api.ListDomainsByMcIdServiceApi;
import org.osc.core.broker.service.api.ListEncapsulationTypeByVersionTypeAndModelApi;
import org.osc.core.broker.service.api.ListVirtualizationConnectorBySwVersionServiceApi;
import org.osc.core.broker.service.dto.ApplianceManagerConnectorDto;
import org.osc.core.broker.service.dto.ApplianceModelSoftwareVersionDto;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.dto.DomainDto;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.dto.VirtualizationType;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.ListApplianceModelSwVersionComboRequest;
import org.osc.core.broker.service.request.ListEncapsulationTypeByVersionTypeAndModelRequest;
import org.osc.core.broker.service.request.ListVirtualizationConnectorBySwVersionRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.button.OkCancelButtonModel;
import org.osc.sdk.controller.TagEncapsulationType;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;

public abstract class BaseDAWindow extends CRUDBaseWindow<OkCancelButtonModel> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(BaseDAWindow.class);
    // form fields
    protected TextField name = null;
    protected ComboBox managerConnector = null;
    protected ComboBox applianceDefinition = null;
    protected Panel attributePanel = null;
    protected Table attributes = null;
    protected PasswordField sharedKey;
    protected Table vsTable = null;
    protected DistributedApplianceDto currentDAObject = null;
    private final ListApplianceModelSwVersionComboServiceApi listApplianceModelSwVersionComboService;
    private final ListDomainsByMcIdServiceApi listDomainsByMcIdService;
    private final ListEncapsulationTypeByVersionTypeAndModelApi listEncapsulationTypeByVersionTypeAndModel;
    private final ListApplianceManagerConnectorServiceApi listApplianceManagerConnectorService;
    private final ListVirtualizationConnectorBySwVersionServiceApi listVirtualizationConnectorBySwVersionService;

    public BaseDAWindow(ListApplianceModelSwVersionComboServiceApi listApplianceModelSwVersionComboService,
            ListDomainsByMcIdServiceApi listDomainsByMcIdService,
            ListEncapsulationTypeByVersionTypeAndModelApi listEncapsulationTypeByVersionTypeAndModel,
            ListApplianceManagerConnectorServiceApi listApplianceManagerConnectorService,
            ListVirtualizationConnectorBySwVersionServiceApi listVirtualizationConnectorBySwVersionService) {
        super();
        this.listApplianceModelSwVersionComboService = listApplianceModelSwVersionComboService;
        this.listDomainsByMcIdService = listDomainsByMcIdService;
        this.listEncapsulationTypeByVersionTypeAndModel = listEncapsulationTypeByVersionTypeAndModel;
        this.listApplianceManagerConnectorService = listApplianceManagerConnectorService;
        this.listVirtualizationConnectorBySwVersionService = listVirtualizationConnectorBySwVersionService;
    }

    protected Panel getAttributesPanel() {

        this.sharedKey = new PasswordField();
        this.sharedKey.setRequiredError("shared secret key cannot be empty");
        this.sharedKey.setRequired(true);
        // best show/hide this conditionally based on Manager type.
        this.sharedKey.setValue("dummy1234");

        this.attributes = new Table();
        this.attributes.setPageLength(0);
        this.attributes.setSelectable(true);
        this.attributes.setSizeFull();
        this.attributes.setImmediate(true);

        this.attributes.addContainerProperty("Attribute Name", String.class, null);
        this.attributes.addContainerProperty("Value", PasswordField.class, null);
        this.attributes.addItem(new Object[] { "Shared Secret key", this.sharedKey }, new Integer(1));
        // creating panel to store attributes table
        this.attributePanel = new Panel("Common Appliance Configuration Attributes:");
        this.attributePanel.addStyleName("form_Panel");
        this.attributePanel.setWidth(100, Sizeable.Unit.PERCENTAGE);
        this.attributePanel.setContent(this.attributes);

        return this.attributePanel;
    }

    /**
     * @return AZ Panel
     */
    @SuppressWarnings("serial")
    protected Panel getVirtualSystemPanel() {
        try {

            this.vsTable = new Table();
            this.vsTable.setPageLength(5);
            this.vsTable.setImmediate(true);
            this.vsTable.addGeneratedColumn("Enabled", new CheckBoxGenerator());
            this.vsTable.addItemClickListener(new ItemClickListener() {
                @Override
                public void itemClick(ItemClickEvent event) {
                    vsTableClicked((Long) event.getItemId());
                }
            });

            // populating VS table
            populateVirtualSystem();

            Panel vsPanel = new Panel("Virtualization System:");
            vsPanel.addStyleName("form_Panel");
            vsPanel.setWidth(100, Sizeable.Unit.PERCENTAGE);
            vsPanel.setContent(this.vsTable);

            return vsPanel;

        } catch (Exception e) {

            log.error("Error while creating DA's VS panel", e);
        }

        return null;
    }

    protected void populateVirtualSystem() throws Exception {
        ApplianceModelSoftwareVersionDto currentAppliance = (ApplianceModelSoftwareVersionDto) this.applianceDefinition
                .getValue();

        // List VC Service
        ListVirtualizationConnectorBySwVersionRequest vcRequest = new ListVirtualizationConnectorBySwVersionRequest();
        if (currentAppliance != null) {
            vcRequest.setSwVersion(currentAppliance.getSwVersion());
        }

        ListResponse<VirtualizationConnectorDto> vcResponse = this.listVirtualizationConnectorBySwVersionService.dispatch(vcRequest);

        ApplianceManagerConnectorDto currentMC = (ApplianceManagerConnectorDto) this.managerConnector.getValue();

        // creating Virtual System Table
        this.vsTable.addContainerProperty("Enabled", Boolean.class, false);
        this.vsTable.addContainerProperty("Virtualization Connector", String.class, null);
        this.vsTable.addContainerProperty("Type", String.class, null);
        this.vsTable.addContainerProperty("Manager Domain", ComboBox.class, null);
        this.vsTable.addContainerProperty("Encapsulation Type", ComboBox.class, null);

        List<DomainDto> dl = getDomainList(currentMC);

        this.vsTable.removeAllItems();

        for (VirtualizationConnectorDto vc : vcResponse.getList()) {
            ComboBox domainComboBox = createDomainComboBox(dl);
            ComboBox encapsulationTypeComboBox = createEncapsulationTypeComboBox(vc.getType(),
                    getEncapsulationType(currentAppliance, vc.getType()));

            //get Encapsulation Type for appliance

            // adding new row to vs table
            this.vsTable.addItem(new Object[] { vc.getName(), vc.getType().toString(), domainComboBox,
                    encapsulationTypeComboBox }, vc.getId());
        }
    }

    private List<TagEncapsulationType> getEncapsulationType(ApplianceModelSoftwareVersionDto currentAppliance,
            VirtualizationType type) throws Exception {
        ListEncapsulationTypeByVersionTypeAndModelRequest req = new ListEncapsulationTypeByVersionTypeAndModelRequest(
                currentAppliance.getSwVersion(), currentAppliance.getApplianceModel(), type);
        return this.listEncapsulationTypeByVersionTypeAndModel.dispatch(req).getList();
    }

    protected ComboBox getDomainComboBox(Object id) {
        return (ComboBox) this.vsTable.getItem(id).getItemProperty("Manager Domain").getValue();
    }

    protected ComboBox getEncapsulationTypeComboBox(Object id) {
        return (ComboBox) this.vsTable.getItem(id).getItemProperty("Encapsulation Type").getValue();
    }

    @SuppressWarnings("unchecked")
    private void vsTableClicked(long itemId) {
        if (this.vsTable.getContainerProperty(itemId, "Enabled").getValue().equals(true)) {
            this.vsTable.getContainerProperty(itemId, "Enabled").setValue(false);
        } else {
            this.vsTable.getContainerProperty(itemId, "Enabled").setValue(true);
        }
    }

    /**
     * @return MC ComboBox
     */
    @SuppressWarnings("serial")
    protected ComboBox getManagerConnector() {
        try {
            ListResponse<ApplianceManagerConnectorDto> res = this.listApplianceManagerConnectorService.dispatch(new BaseRequest<>());

            BeanItemContainer<ApplianceManagerConnectorDto> mcList = new BeanItemContainer<ApplianceManagerConnectorDto>(
                    ApplianceManagerConnectorDto.class, res.getList());
            this.managerConnector = new ComboBox("Manager Connector");
            this.managerConnector.setTextInputAllowed(false);
            this.managerConnector.setNullSelectionAllowed(false);
            this.managerConnector.setContainerDataSource(mcList);
            this.managerConnector.setItemCaptionPropertyId("name");

            if (mcList.size() > 0) {
                this.managerConnector.select(mcList.getIdByIndex(0));
            }

            this.managerConnector.setImmediate(true);
            this.managerConnector.setRequired(true);
            this.managerConnector.setRequiredError("Manager Connector cannot be empty");

            this.managerConnector.addValueChangeListener(new ValueChangeListener() {

                @Override
                public void valueChange(ValueChangeEvent event) {
                    ApplianceManagerConnectorDto mcDto = (ApplianceManagerConnectorDto) BaseDAWindow.this.managerConnector
                            .getValue();
                    updateAppliances();
                    updateDomains(mcDto);
                }
            });

        } catch (Exception e) {
            log.error("Error populating MC combobox", e);
        }

        return this.managerConnector;

    }

    @SuppressWarnings("serial")
    protected void updateAppliances() {
        ApplianceManagerConnectorDto currentMC = (ApplianceManagerConnectorDto) this.managerConnector.getValue();
        if (currentMC != null) {
            ListApplianceModelSwVersionComboRequest adRequest = new ListApplianceModelSwVersionComboRequest();
            adRequest.setType(currentMC.getManagerType());
            ListResponse<ApplianceModelSoftwareVersionDto> adResponse = null;
            try {
                adResponse = this.listApplianceModelSwVersionComboService.dispatch(adRequest);
                BeanItemContainer<ApplianceModelSoftwareVersionDto> adList = new BeanItemContainer<ApplianceModelSoftwareVersionDto>(
                        ApplianceModelSoftwareVersionDto.class, adResponse.getList());

                this.applianceDefinition.setContainerDataSource(adList);
                this.applianceDefinition.setItemCaptionPropertyId("name");
                if (adList.size() > 0) {
                    this.applianceDefinition.select(adList.getIdByIndex(0));
                }
                this.applianceDefinition.addValueChangeListener(new ValueChangeListener() {

                    @Override
                    public void valueChange(ValueChangeEvent event) {
                        try {
                            populateVirtualSystem();
                        } catch (Exception e) {
                            log.error("Error while populating Virtual System Table ", e);
                        }
                    }
                });

            } catch (Exception e) {
                log.error("Error retrieving appliance list", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void updateDomains(ApplianceManagerConnectorDto mcDto) {

        List<DomainDto> dl = getDomainList(mcDto);

        for (Object itemId : this.vsTable.getItemIds()) {
            ComboBox domainComboBox = createDomainComboBox(dl);

            Item item = this.vsTable.getItem(itemId);
            item.getItemProperty("Manager Domain").setValue(domainComboBox);
        }
    }

    private ComboBox createDomainComboBox(List<DomainDto> dl) {
        ComboBox domainComboBox = new ComboBox();
        BeanItemContainer<DomainDto> domainContainer = new BeanItemContainer<DomainDto>(DomainDto.class, dl);
        ApplianceManagerConnectorDto mc = (ApplianceManagerConnectorDto) this.managerConnector.getValue();

        domainComboBox.setContainerDataSource(domainContainer);
        domainComboBox.setTextInputAllowed(false);
        domainComboBox.setNullSelectionAllowed(false);
        domainComboBox.setItemCaptionPropertyId("name");
        domainComboBox.setEnabled(mc.isPolicyMappingSupported());

        if (domainComboBox.getItemIds().size() > 0) {
            domainComboBox.select(domainContainer.getIdByIndex(0));
        }
        return domainComboBox;
    }

    private ComboBox createEncapsulationTypeComboBox(VirtualizationType virtualizationType,
            List<TagEncapsulationType> types) {
        ComboBox encapsulationType = new ComboBox();
        encapsulationType.setTextInputAllowed(false);
        encapsulationType.setNullSelectionAllowed(true);

        BeanItemContainer<TagEncapsulationType> encapsulationTypeContainer = new BeanItemContainer<TagEncapsulationType>(
                TagEncapsulationType.class, types);
        encapsulationType.setContainerDataSource(encapsulationTypeContainer);
        ApplianceManagerConnectorDto currentMc = (ApplianceManagerConnectorDto) this.managerConnector.getValue();

        if (!virtualizationType.isOpenstack() || (currentMc != null && !currentMc.isPolicyMappingSupported())) {
            encapsulationType.setEnabled(false);
        }
        return encapsulationType;
    }

    /**
     * @return AD ComboBox
     */
    protected ComboBox getApplianceDefinition() {
        try {
            this.applianceDefinition = new ComboBox("Service Function Definition");
            this.applianceDefinition.setTextInputAllowed(false);
            this.applianceDefinition.setNullSelectionAllowed(false);
            this.applianceDefinition.setRequired(true);
            this.applianceDefinition.setRequiredError("Service Function Definition cannot be Empty");
            this.applianceDefinition.setWidth(100, Unit.PERCENTAGE);

            updateAppliances();
        } catch (Exception e) {
            log.error("Error populating appliance list combobox", e);
        }

        return this.applianceDefinition;
    }

    protected List<DomainDto> getDomainList(ApplianceManagerConnectorDto mc) {
        if (mc != null) {
            ListResponse<DomainDto> response = new ListResponse<DomainDto>();
            try {
                // List Domains Service
                BaseIdRequest agRequest = new BaseIdRequest();
                agRequest.setId(mc.getId());
                response = this.listDomainsByMcIdService.dispatch(agRequest);
            } catch (Exception e) {
                log.error("Error populating domain combobox", e);
            }
            return response.getList();
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public boolean validateForm() {
        try {
            this.name.validate();
            if (!ValidateUtil.validateDaName(this.name.getValue().toString())) {
                ViewUtil.iscNotification(
                        "DA name must not exceed 13 characters, must start with a letter,  and can only contain numbers, letters and dash(-).",
                        Notification.Type.ERROR_MESSAGE);
                return false;
            }
            this.managerConnector.validate();
            this.applianceDefinition.validate();
            this.sharedKey.validate();
            int rowCount = 0;
            ApplianceManagerConnectorDto mcDto = (ApplianceManagerConnectorDto) this.managerConnector.getValue();

            for (Object id : this.vsTable.getItemIds()) {
                if (this.vsTable.getContainerProperty(id, "Enabled").getValue().equals(true)) {
                    rowCount++;

                    DomainDto domainDto = (DomainDto) getDomainComboBox(id).getValue();
                    if (domainDto == null && mcDto.isPolicyMappingSupported()) {
                        ViewUtil.iscNotification("Please ensure domain is selected.", Notification.Type.ERROR_MESSAGE);
                        return false;
                    }
                    VirtualizationType vsType = VirtualizationType.fromText((String) this.vsTable.getContainerProperty(
                            id, "Type").getValue());
                    TagEncapsulationType tag = (TagEncapsulationType) getEncapsulationTypeComboBox(id).getValue();
                    if (vsType.isOpenstack() && mcDto.isPolicyMappingSupported()) {
                        if (tag == null) {
                            ViewUtil.iscNotification("Please ensure Encapsulation type is selected.",
                                    Notification.Type.ERROR_MESSAGE);
                            return false;
                        }
                    } else {
                        if (tag != null) {
                            ViewUtil.iscNotification(
                                    "Please ensure Encapsulation type is selected only for Openstack Virtual Systems.",
                                    Notification.Type.ERROR_MESSAGE);
                            return false;
                        }
                    }

                }
            }
            if (rowCount <= 0) {
                ViewUtil.iscNotification("Please select one or more Virtualization System for creating this DA.",
                        Notification.Type.ERROR_MESSAGE);
                return false;
            }

            return true;
        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage() + ".", Notification.Type.ERROR_MESSAGE);
        }
        return false;
    }

    @SuppressWarnings("serial")
    private class CheckBoxGenerator implements Table.ColumnGenerator {
        @Override
        public Object generateCell(Table source, Object itemId, Object columnId) {
            Property<?> prop = source.getItem(itemId).getItemProperty(columnId);
            return new CheckBox(null, prop);
        }
    }

}
