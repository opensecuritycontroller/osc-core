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
package org.osc.core.broker.window.update;

import java.util.HashSet;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.UpdateDistributedApplianceServiceApi;
import org.osc.core.broker.service.dto.ApplianceManagerConnectorDto;
import org.osc.core.broker.service.dto.ApplianceModelSoftwareVersionDto;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.dto.DomainDto;
import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.view.DistributedApplianceView;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.BaseDAWindow;
import org.osc.core.broker.window.VmidcWindow;
import org.osc.core.broker.window.WindowUtil;
import org.osc.core.broker.window.button.OkCancelButtonModel;
import org.osc.sdk.controller.TagEncapsulationType;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class UpdateDistributedApplianceWindow extends BaseDAWindow {

    private static final Logger log = Logger.getLogger(UpdateDistributedApplianceWindow.class);
    final String CAPTION = "Edit Distributed Appliance";

    private final DistributedApplianceView daView;

    private UpdateDistributedApplianceServiceApi updateDistributedApplianceServiceApi;

    public UpdateDistributedApplianceWindow(DistributedApplianceView distributedApplianceView,
            UpdateDistributedApplianceServiceApi updateDistributedApplianceServiceApi) throws Exception {
        super();
        this.daView = distributedApplianceView;
        this.updateDistributedApplianceServiceApi = updateDistributedApplianceServiceApi;
        this.currentDAObject = this.daView.getParentContainer().getItem(this.daView.getParentItemId()).getBean();
        createWindow(this.CAPTION);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void populateForm() {
        try {
            this.name = new TextField("Name");
            this.name.setImmediate(true);
            this.name.focus();
            this.name.setRequired(true);
            this.name.setEnabled(false);
            this.name.setRequiredError("Name cannot be empty");

            FormLayout innerForm = new FormLayout();
            innerForm.addComponent(this.name);
            innerForm.addComponent(getManagerConnector());
            innerForm.addComponent(getApplianceDefinition());
            innerForm.setWidth(100, Unit.PERCENTAGE);

            VerticalLayout layout = new VerticalLayout();
            layout.addComponent(innerForm);
            // TODO: Future. Need to dynamically show/hide attributes based on manager type
            getAttributesPanel();
            layout.addComponent(getVirtualSystemPanel());

            this.name.setValue(this.currentDAObject.getName());
            // this.sharedKey.setValue(this.currentDAObject.getSecretKey());

            // manager connecter drop down with the existing value
            BeanItemContainer<ApplianceManagerConnectorDto> mcContainer = (BeanItemContainer<ApplianceManagerConnectorDto>) this.managerConnector
                    .getContainerDataSource();
            for (ApplianceManagerConnectorDto mc : mcContainer.getItemIds()) {
                if (mc.getId().equals(this.currentDAObject.getMcId())) {
                    this.managerConnector.select(mc);
                }
            }
            this.managerConnector.setEnabled(false);

            // appliance definition drop down with existing value
            setApplianceDefinitionToCurrent();

            // select existing virtual systems for this DA
            selectExistingVistualSytems();

            this.form.setMargin(true);
            this.form.setWidth(689, Unit.PIXELS);
            this.form.addComponent(layout);

            this.applianceDefinition.addValueChangeListener(new ValueChangeListener() {
                @Override
                public void valueChange(ValueChangeEvent event) {
                    selectExistingVistualSytems();
                }
            });

        } catch (Exception e) {
            log.error("Fail to load DA Form", e);
            ViewUtil.iscNotification("Fail load Distributed Appliance Form (" + e.getMessage() + ")",
                    Notification.Type.ERROR_MESSAGE);
        }
    }

    @SuppressWarnings("unchecked")
    private void selectExistingVistualSytems() {
        for (Object id : this.vsTable.getItemIds()) {
            for (VirtualSystemDto vsDto : this.currentDAObject.getVirtualizationSystems()) {
                if (vsDto.getVirtualizationConnectorName().equals(
                        this.vsTable.getItem(id).getItemProperty("Virtualization Connector").getValue())) {
                    BeanItemContainer<DomainDto> domainContianer = (BeanItemContainer<DomainDto>) getDomainComboBox(id)
                            .getContainerDataSource();
                    for (DomainDto domainDto : domainContianer.getItemIds()) {
                        if (domainDto.getId().equals(vsDto.getDomainId())) {
                            getDomainComboBox(id).select(domainDto);
                            getDomainComboBox(id).setEnabled(false);
                        }
                    }
                    BeanItemContainer<TagEncapsulationType> typeContainer = (BeanItemContainer<TagEncapsulationType>) getEncapsulationTypeComboBox(
                            id).getContainerDataSource();
                    for (TagEncapsulationType type : typeContainer.getItemIds()) {
                        if (type == vsDto.getEncapsulationType()) {
                            getEncapsulationTypeComboBox(id).select(type);
                            getEncapsulationTypeComboBox(id).setEnabled(false);
                        }
                    }
                    this.vsTable.getItem(id).getItemProperty("Enabled").setValue(true);
                    if (vsDto.isMarkForDeletion()) {
                        this.vsTable.getItem(id).getItemProperty("Enabled").setReadOnly(true);
                    }
                }
            }
        }
    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                String currentDaVersion = this.currentDAObject.getApplianceModel() + "-"
                        + this.currentDAObject.getApplianceSoftwareVersionName();
                String selectedVersion = ((ApplianceModelSoftwareVersionDto) this.applianceDefinition.getValue())
                        .getName();
                if (!selectedVersion.equals(currentDaVersion)) {
                    VmidcWindow<OkCancelButtonModel> alertWindow = getUpgradeAlertWindow();
                    ViewUtil.addWindow(alertWindow);
                } else {
                    submitRequest();
                }
            }
        } catch (Exception e) {
            handleCatchAllException(e);
        }
    }

    private void submitRequest() {
        try {
            HashSet<VirtualSystemDto> vsSet = new HashSet<VirtualSystemDto>();
            ApplianceManagerConnectorDto mcValue = (ApplianceManagerConnectorDto) this.managerConnector.getValue();
            ApplianceModelSoftwareVersionDto applianceDefinitionValue = (ApplianceModelSoftwareVersionDto) this.applianceDefinition
                    .getValue();
            for (Object id : this.vsTable.getItemIds()) {
                if (this.vsTable.getContainerProperty(id, "Enabled").getValue().equals(true)) {
                    VirtualSystemDto vsDto = new VirtualSystemDto();
                    // filling AG information
                    DomainDto domainDto = (DomainDto) getDomainComboBox(id).getValue();
                    vsDto.setDomainId(domainDto.getId());
                    TagEncapsulationType type = (TagEncapsulationType) getEncapsulationTypeComboBox(id).getValue();
                    vsDto.setEncapsulationType(type);
                    // filling VC information
                    vsDto.setVcId((long) id);
                    // merging new VS list with existing one
                    for (VirtualSystemDto existingVs : this.currentDAObject.getVirtualizationSystems()) {
                        if (existingVs.getVcId().equals(vsDto.getVcId())
                                && existingVs.getDomainId().equals(vsDto.getDomainId())) {
                            vsDto.setId(existingVs.getId());
                        }
                    }
                    vsSet.add(vsDto);
                }
            }

            BaseRequest<DistributedApplianceDto> updateRequest = new BaseRequest<DistributedApplianceDto>();
            updateRequest.setDto(new DistributedApplianceDto());

            updateRequest.getDto().setId(this.currentDAObject.getId());
            updateRequest.getDto().setName(this.name.getValue().trim());
            updateRequest.getDto().setSecretKey(this.sharedKey.getValue().trim());
            updateRequest.getDto().setMcId(mcValue.getId());

            updateRequest.getDto().setApplianceId(applianceDefinitionValue.getApplianceId());
            updateRequest.getDto().setApplianceSoftwareVersionName(applianceDefinitionValue.getSwVersion());
            updateRequest.getDto().setApplianceModel(applianceDefinitionValue.getApplianceModel());
            updateRequest.getDto().setVirtualizationSystems(vsSet);

            BaseJobResponse response = this.updateDistributedApplianceServiceApi.dispatch(updateRequest);

            close();

            ViewUtil.showJobNotification(response.getJobId());

        } catch (Exception e) {
            handleCatchAllException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void setApplianceDefinitionToCurrent() {
        BeanItemContainer<ApplianceModelSoftwareVersionDto> adContainer = (BeanItemContainer<ApplianceModelSoftwareVersionDto>) this.applianceDefinition
                .getContainerDataSource();
        for (ApplianceModelSoftwareVersionDto ad : adContainer.getItemIds()) {
            if ((this.currentDAObject.getApplianceModel() + "-" + this.currentDAObject.getApplianceSoftwareVersionName()).equals(ad
                    .getName())) {
                this.applianceDefinition.select(ad);
            }
        }
    }

    private VmidcWindow<OkCancelButtonModel> getUpgradeAlertWindow() {
        String fromVersion = this.currentDAObject.getApplianceModel() + "-" + this.currentDAObject.getApplianceSoftwareVersionName();
        String toVersion = ((ApplianceModelSoftwareVersionDto) this.applianceDefinition.getValue()).getName();
        final VmidcWindow<OkCancelButtonModel> upgradeAlert = WindowUtil.createAlertWindow(
                VmidcMessages.getString(VmidcMessages_.DA_UPGRADE_WARNING_TITLE),
                VmidcMessages.getString(VmidcMessages_.DA_UPGRADE_WARNING, fromVersion, toVersion));
        upgradeAlert.getComponentModel().setOkClickedListener(new ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
                upgradeAlert.close();
                submitRequest();
            }
        });
        return upgradeAlert;
    }
}
