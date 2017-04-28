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
package org.osc.core.broker.window.add;

import java.util.HashSet;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.AddDistributedApplianceServiceApi;
import org.osc.core.broker.service.api.ListApplianceManagerConnectorServiceApi;
import org.osc.core.broker.service.api.ListApplianceModelSwVersionComboServiceApi;
import org.osc.core.broker.service.api.ListDomainsByMcIdServiceApi;
import org.osc.core.broker.service.api.ListEncapsulationTypeByVersionTypeAndModelApi;
import org.osc.core.broker.service.dto.ApplianceManagerConnectorDto;
import org.osc.core.broker.service.dto.ApplianceModelSoftwareVersionDto;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.dto.DomainDto;
import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.AddDistributedApplianceResponse;
import org.osc.core.broker.view.DistributedApplianceView;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.BaseDAWindow;
import org.osc.sdk.controller.TagEncapsulationType;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class AddDistributedApplianceWindow extends BaseDAWindow {

    final String CAPTION = "Add Distributed Appliance";

    private static final Logger log = Logger.getLogger(DistributedApplianceView.class);
    // current view reference
    private DistributedApplianceView daView = null;

    private AddDistributedApplianceServiceApi addDistributedApplianceService;

    public AddDistributedApplianceWindow(DistributedApplianceView distributedApplianceView,
            AddDistributedApplianceServiceApi addDistributedApplianceService,
            ListApplianceModelSwVersionComboServiceApi listApplianceModelSwVersionComboService,
            ListDomainsByMcIdServiceApi listDomainsByMcIdService,
            ListEncapsulationTypeByVersionTypeAndModelApi listEncapsulationTypeByVersionTypeAndModel,
            ListApplianceManagerConnectorServiceApi listApplianceManagerConnectorService) throws Exception {
        super(listApplianceModelSwVersionComboService, listDomainsByMcIdService, listEncapsulationTypeByVersionTypeAndModel,
                listApplianceManagerConnectorService);
        this.daView = distributedApplianceView;
        this.addDistributedApplianceService = addDistributedApplianceService;
        createWindow(this.CAPTION);
    }

    @Override
    public void populateForm() {

        try {
            this.name = new TextField("Name");
            this.name.focus();
            this.name.setImmediate(true);
            this.name.setRequired(true);
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

            this.form.setMargin(true);
            this.form.setWidth(689, Unit.PIXELS);
            this.form.addComponent(layout);

        } catch (Exception e) {
            log.error("Fail to populate Distributed Appliance form", e);
            ViewUtil.iscNotification("Fail to populate Distributed Appliance form (" + e.getMessage() + ")",
                    Notification.Type.ERROR_MESSAGE);
        }
    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {

                HashSet<VirtualSystemDto> vsSet = new HashSet<VirtualSystemDto>();
                ApplianceManagerConnectorDto mcValue = (ApplianceManagerConnectorDto) this.managerConnector.getValue();
                ApplianceModelSoftwareVersionDto adValue = (ApplianceModelSoftwareVersionDto) this.applianceDefinition
                        .getValue();

                for (Object id : this.vsTable.getItemIds()) {
                    if (this.vsTable.getContainerProperty(id, "Enabled").getValue().equals(true)) {
                        VirtualSystemDto vsDto = new VirtualSystemDto();
                        // filling AG information
                        DomainDto domainDto = (DomainDto) getDomainComboBox(id).getValue();
                        vsDto.setDomainId(domainDto == null ? null : domainDto.getId());
                        TagEncapsulationType type = (TagEncapsulationType) getEncapsulationTypeComboBox(id).getValue();
                        vsDto.setEncapsulationType(type);

                        // filling VC information
                        vsDto.setVcId((long) id);
                        vsSet.add(vsDto);
                    }
                }

                BaseRequest<DistributedApplianceDto> addRequest = new BaseRequest<DistributedApplianceDto>();

                DistributedApplianceDto daDto = new DistributedApplianceDto();
                daDto.setName(this.name.getValue().trim());
                daDto.setSecretKey(this.sharedKey.getValue().trim());
                daDto.setMcId(mcValue.getId());
                daDto.setApplianceId(adValue.getApplianceId());
                daDto.setApplianceSoftwareVersionName(adValue.getSwVersion());
                daDto.setVirtualizationSystems(vsSet);
                addRequest.setDto(daDto);

                AddDistributedApplianceResponse addResponse = this.addDistributedApplianceService
                        .dispatch(addRequest);

                this.daView.getParentContainer().addItemAt(0, addResponse.getId(), addResponse);
                this.daView.parentTableClicked(addResponse.getId());

                close();

                ViewUtil.showJobNotification(addResponse.getJobId());

            }
        } catch (Exception e) {
            log.error("Fail to add DA", e);
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }

    }
}
