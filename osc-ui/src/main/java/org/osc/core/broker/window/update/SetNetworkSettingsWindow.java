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

import org.osc.core.broker.service.api.SetNetworkSettingsServiceApi;
import org.osc.core.broker.service.request.SetNetworkSettingsRequest;
import org.osc.core.broker.view.maintenance.NetworkLayout;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.CRUDBaseWindow;
import org.osc.core.broker.window.button.OkCancelButtonModel;
import org.osc.core.ui.LogProvider;
import org.slf4j.Logger;

import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;

public class SetNetworkSettingsWindow extends CRUDBaseWindow<OkCancelButtonModel> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LogProvider.getLogger(SetNetworkSettingsWindow.class);

    final String CAPTION = "Set Network Settings";

    private TextField ipAddress = null;
    private TextField subnetMask = null;
    private TextField defaultGateway = null;
    private TextField dnsServer1 = null;
    private TextField dnsServer2 = null;

    private final NetworkLayout networkLayout;

    private final SetNetworkSettingsServiceApi setNetworkSettingsService;

    public SetNetworkSettingsWindow(NetworkLayout networkLayout,
            SetNetworkSettingsServiceApi setNetworkSettingsService) throws Exception {
        super();
        this.networkLayout = networkLayout;
        this.setNetworkSettingsService = setNetworkSettingsService;
        createWindow(this.CAPTION);

    }

    @Override
    public void populateForm() {
        this.ipAddress = new TextField("IPv4 Address");
        this.ipAddress.setImmediate(true);
        this.subnetMask = new TextField("Netmask");
        this.subnetMask.setImmediate(true);
        this.defaultGateway = new TextField("Default Gateway");
        this.defaultGateway.setImmediate(true);
        this.dnsServer1 = new TextField("Primary DNS Server");
        this.dnsServer2 = new TextField("Secondary DNS Server");

        // filling form with existing data
        if (this.networkLayout.networkTable.getItem(1).getItemProperty("Value").getValue() != null) {
            this.ipAddress.setValue(
                    this.networkLayout.networkTable.getItem(1).getItemProperty("Value").getValue().toString());
        }
        if (this.networkLayout.networkTable.getItem(2).getItemProperty("Value").getValue() != null) {
            this.subnetMask.setValue(
                    this.networkLayout.networkTable.getItem(2).getItemProperty("Value").getValue().toString());
        }
        if (this.networkLayout.networkTable.getItem(3).getItemProperty("Value").getValue() != null) {
            this.defaultGateway.setValue(
                    this.networkLayout.networkTable.getItem(3).getItemProperty("Value").getValue().toString());
        }
        if (this.networkLayout.networkTable.getItem(4).getItemProperty("Value").getValue() != null) {
            this.dnsServer1.setValue(
                    this.networkLayout.networkTable.getItem(4).getItemProperty("Value").getValue().toString());
        }
        if (this.networkLayout.networkTable.getItem(5).getItemProperty("Value").getValue() != null) {
            this.dnsServer2.setValue(
                    this.networkLayout.networkTable.getItem(5).getItemProperty("Value").getValue().toString());
        }

        // adding not null constraint
        this.ipAddress.setRequired(true);
        this.ipAddress.setRequiredError("IPv4 Address cannot be empty");
        this.subnetMask.setRequired(true);
        this.subnetMask.setRequiredError("Netmask cannot be empty");
        this.defaultGateway.setRequired(true);
        this.defaultGateway.setRequiredError("Default Gateway cannot be empty");

        this.form.addComponent(this.ipAddress);
        this.ipAddress.focus();
        this.form.addComponent(this.subnetMask);
        this.form.addComponent(this.defaultGateway);
        this.form.addComponent(this.dnsServer1);
        this.form.addComponent(this.dnsServer2);

    }

    @Override
    public boolean validateForm() {
        try {
            this.ipAddress.validate();
            this.subnetMask.validate();
            this.defaultGateway.validate();
            if (this.networkLayout.networkTable.getItem(1).getItemProperty("Value").getValue()
                    .equals(this.ipAddress.getValue())
                    && this.networkLayout.networkTable.getItem(2).getItemProperty("Value").getValue()
                            .equals(this.subnetMask.getValue())
                    && this.networkLayout.networkTable.getItem(3).getItemProperty("Value").getValue()
                            .equals(this.defaultGateway.getValue())
                    && this.networkLayout.networkTable.getItem(4).getItemProperty("Value").getValue()
                            .equals(this.dnsServer1.getValue())
                    && this.networkLayout.networkTable.getItem(5).getItemProperty("Value").getValue()
                            .equals(this.dnsServer2.getValue())) {
                return false;
            }
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
                SetNetworkSettingsRequest req = new SetNetworkSettingsRequest();
                req.setDhcp(false);
                req.setHostIpAddress(this.ipAddress.getValue().trim());
                req.setHostDefaultGateway(this.defaultGateway.getValue().trim());
                req.setHostSubnetMask(this.subnetMask.getValue().trim());
                req.setHostDnsServer1(this.dnsServer1.getValue().trim());
                req.setHostDnsServer2(this.dnsServer2.getValue().trim());

                this.setNetworkSettingsService.dispatch(req);

                this.networkLayout.populateNetworkTable();
            }
            close();
        } catch (Exception e) {
            log.error("Failed to update the network settings", e);
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }

    }
}
