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

import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.SetNATSettingsServiceApi;
import org.osc.core.broker.service.api.server.ValidationApi;
import org.osc.core.broker.service.dto.NATSettingsDto;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.view.maintenance.NetworkLayout;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.CRUDBaseWindow;
import org.osc.core.broker.window.button.OkCancelButtonModel;

import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;

public class SetNATSettingsWindow extends CRUDBaseWindow<OkCancelButtonModel> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(SetNetworkSettingsWindow.class);

    final String CAPTION = "Set Network Settings";

    private TextField ipAddress = null;

    private final NetworkLayout networkLayout;

    private final SetNATSettingsServiceApi setNATSettingsService;

    private final ValidationApi validator;

    public SetNATSettingsWindow(NetworkLayout networkLayout, SetNATSettingsServiceApi setNATSettingsService,
            ValidationApi validator) throws Exception {
        super();
        this.networkLayout = networkLayout;
        this.setNATSettingsService = setNATSettingsService;
        this.validator = validator;
        createWindow(this.CAPTION);

    }

    @Override
    public void populateForm() {
        this.ipAddress = new TextField("IPv4 Address");
        this.ipAddress.setImmediate(true);

        // filling form with existing data
        if (this.networkLayout.natTable.getItem(1).getItemProperty("Value").getValue() != null) {
            this.ipAddress.setValue(this.networkLayout.natTable.getItem(1).getItemProperty("Value").getValue()
                    .toString());
        }

        // adding not null constraint
        this.ipAddress.setRequired(true);
        this.ipAddress.setRequiredError("IPv4 Address cannot be empty");

        this.form.addComponent(this.ipAddress);
        this.ipAddress.focus();

    }

    @Override
    public boolean validateForm() {
        try {
            this.ipAddress.validate();
            this.validator.checkValidIpAddress(this.ipAddress.getValue());
            return true;
        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage() + ".", Notification.Type.ERROR_MESSAGE);
        }

        return false;
    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                NATSettingsDto dto = new NATSettingsDto(this.ipAddress.getValue());
                DryRunRequest<NATSettingsDto> req = new DryRunRequest<NATSettingsDto>();
                req.setDto(dto);

                BaseJobResponse response = this.setNATSettingsService.dispatch(req);
                this.networkLayout.populateNATTable();
                if (response.getJobId() != null) {
                    ViewUtil.showJobNotification(response.getJobId());
                }
                close();
            }
        } catch (Exception e) {
            log.error("Failed to update the NAT settings", e);
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }

    }
}
