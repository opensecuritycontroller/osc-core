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
package org.osc.core.broker.view.maintenance;

import org.osc.core.broker.service.api.CheckNetworkSettingsServiceApi;
import org.osc.core.broker.service.api.GetNATSettingsServiceApi;
import org.osc.core.broker.service.api.GetNetworkSettingsServiceApi;
import org.osc.core.broker.service.api.SetNATSettingsServiceApi;
import org.osc.core.broker.service.api.SetNetworkSettingsServiceApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.api.server.ValidationApi;
import org.osc.core.broker.service.dto.NATSettingsDto;
import org.osc.core.broker.service.request.GetNetworkSettingsRequest;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.BaseDtoResponse;
import org.osc.core.broker.service.response.CheckNetworkSettingResponse;
import org.osc.core.broker.service.response.GetNetworkSettingsResponse;
import org.osc.core.broker.view.common.StyleConstants;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.VmidcWindow;
import org.osc.core.broker.window.WindowUtil;
import org.osc.core.broker.window.button.OkCancelButtonModel;
import org.osc.core.broker.window.update.SetNATSettingsWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

public class NetworkLayout extends FormLayout {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(NetworkLayout.class);

    public Table networkTable = null;
    private Button editNATSettings = null;
    public Table natTable;

    private GetNetworkSettingsServiceApi getNetworkSettingsService;

    private CheckNetworkSettingsServiceApi checkNetworkSettingsService;

    private GetNATSettingsServiceApi getNATSettingsService;

    private SetNATSettingsServiceApi setNATSettingsService;

    private ValidationApi validator;

    private ServerApi server;

    public NetworkLayout(GetNetworkSettingsServiceApi getNetworkSettingsService,
            CheckNetworkSettingsServiceApi checkNetworkSettingsService,
            SetNetworkSettingsServiceApi setNetworkSettingsService,
            GetNATSettingsServiceApi getNATSettingsService,
            SetNATSettingsServiceApi setNATSettingsService,
            ValidationApi validator, ServerApi server) {
        super();
        this.getNetworkSettingsService = getNetworkSettingsService;
        this.checkNetworkSettingsService = checkNetworkSettingsService;
        this.getNATSettingsService = getNATSettingsService;
        this.setNATSettingsService = setNATSettingsService;
        this.server = server;
        this.validator = validator;
        try {

            // creating layout to hold option group and edit button
            HorizontalLayout optionLayout = new HorizontalLayout();
            optionLayout.addStyleName(StyleConstants.COMPONENT_SPACING_TOP_BOTTOM);

            Panel networkPanel = new Panel("IP Details");
            VerticalLayout networkLayout = new VerticalLayout();
            Panel networkPanelDHCP = new Panel("DHCP");
            networkTable = createNetworkTable();
            networkLayout.addComponent(networkTable);
            networkPanelDHCP.setContent(networkLayout);
            networkPanel.setContent(networkPanelDHCP);

            Panel natPanel = new Panel("NAT Details");
            VerticalLayout natLayout = new VerticalLayout();
            HorizontalLayout editNatLayout = new HorizontalLayout();
            HorizontalLayout dhcpLayout = new HorizontalLayout();
            dhcpLayout.addStyleName("network-options");
            dhcpLayout.setEnabled(false);
            dhcpLayout.setImmediate(true);
            optionLayout.addComponent(dhcpLayout);
            editNatLayout.addStyleName(StyleConstants.COMPONENT_SPACING_TOP_BOTTOM);
            editNatLayout.addComponent(createNATEditButton());
            natLayout.addComponent(editNatLayout);

            this.natTable = createNATTable();
            natLayout.addComponent(this.natTable);
            natLayout.addStyleName(StyleConstants.COMPONENT_SPACING_TOP_BOTTOM);
            natPanel.setContent(natLayout);

            // populating Network Information in the Table
            populateNetworkTable();

            // populating NAT information in the Table
            populateNATTable();

            addComponent(networkPanel);
            addComponent(natPanel);

        } catch (Exception ex) {
            log.error("Failed to get network settings", ex);
        }
    }

    @SuppressWarnings("serial")
    private Button createNATEditButton() {
        // creating edit button
        this.editNATSettings = new Button("Edit");
        this.editNATSettings.setEnabled(true);
        this.editNATSettings.addStyleName(StyleConstants.BUTTON_TOOLBAR);
        this.editNATSettings.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                try {
                    editNATSettingsClicked();
                } catch (Exception e) {
                    ViewUtil.showError("Error editing NAT settings", e);
                }
            }
        });
        return this.editNATSettings;
    }

    @SuppressWarnings("serial")
    private void editNATSettingsClicked() throws Exception {
        try {
            if (!hasDeployedInstances()) {
                ViewUtil.addWindow(new SetNATSettingsWindow(this, this.setNATSettingsService, this.validator, this.server));
            } else {
                final VmidcWindow<OkCancelButtonModel> alertWindow = WindowUtil.createAlertWindow(
                        VmidcMessages.getString(VmidcMessages_.NW_CHANGE_WARNING_TITLE),
                        VmidcMessages.getString(VmidcMessages_.NW_CHANGE_WARNING));
                alertWindow.getComponentModel().setOkClickedListener(new ClickListener() {

                    @Override
                    public void buttonClick(ClickEvent event) {
                        alertWindow.close();
                        try {
                            ViewUtil.addWindow(new SetNATSettingsWindow(NetworkLayout.this,
                                    NetworkLayout.this.setNATSettingsService, NetworkLayout.this.validator, NetworkLayout.this.server));
                        } catch (Exception e) {
                            ViewUtil.showError("Error displaying NAT setting window", e);
                        }
                    }
                });
                ViewUtil.addWindow(alertWindow);
            }
        } catch (Exception e) {
            log.error("Failed to check if NAT settings can be changed. Launching edit settings window", e);
            ViewUtil.addWindow(new SetNATSettingsWindow(this, this.setNATSettingsService, this.validator, this.server));
        }
    }

    private boolean hasDeployedInstances() throws Exception {
        CheckNetworkSettingResponse response = this.checkNetworkSettingsService.dispatch(new Request() {
        });
        return response.hasDeployedInstances();
    }

    private Table createNetworkTable() {
        Table table = new Table();
        table.setSizeFull();
        table.setPageLength(0);
        table.setSelectable(false);
        table.setColumnCollapsingAllowed(true);
        table.setColumnReorderingAllowed(true);
        table.setImmediate(true);
        table.setNullSelectionAllowed(false);
        table.setColumnHeaderMode(Table.ColumnHeaderMode.HIDDEN);
        table.addContainerProperty("Name", String.class, null);
        table.addContainerProperty("Value", String.class, null);

        // initializing network table with empty values
        table.addItem(new Object[] { "IPv4 Address: ", "" }, new Integer(1));
        table.addItem(new Object[] { "Netmask:", "" }, new Integer(2));
        table.addItem(new Object[] { "Default Gateway: ", "" }, new Integer(3));
        table.addItem(new Object[] { "Primary DNS Server: ", "" }, new Integer(4));
        table.addItem(new Object[] { "Secondary DNS Server: ", "" }, new Integer(5));
        return table;
    }

    private Table createNATTable() {
        Table table = new Table();
        table.setSizeFull();
        table.setPageLength(0);
        table.setSelectable(false);
        table.setColumnCollapsingAllowed(true);
        table.setColumnReorderingAllowed(true);
        table.setImmediate(true);
        table.setNullSelectionAllowed(false);
        table.setColumnHeaderMode(Table.ColumnHeaderMode.HIDDEN);
        table.addContainerProperty("Name", String.class, null);
        table.addContainerProperty("Value", String.class, null);

        // initializing network table with empty values
        table.addItem(new Object[] { "Public IPv4 Address: ", "" }, new Integer(1));
        return table;
    }

    @SuppressWarnings("unchecked")
    public void populateNetworkTable() {
        try {
            GetNetworkSettingsRequest getNetworkSettingsRequest = new GetNetworkSettingsRequest();

            GetNetworkSettingsResponse getNetworkSettingsResponse = this.getNetworkSettingsService
                    .dispatch(getNetworkSettingsRequest);

            this.networkTable.getItem(1).getItemProperty("Value")
                    .setValue(getNetworkSettingsResponse.getHostIpAddress());
            this.networkTable.getItem(2).getItemProperty("Value")
                    .setValue(getNetworkSettingsResponse.getHostSubnetMask());
            this.networkTable.getItem(3).getItemProperty("Value")
                    .setValue(getNetworkSettingsResponse.getHostDefaultGateway());
            this.networkTable.getItem(4).getItemProperty("Value")
                    .setValue(getNetworkSettingsResponse.getHostDnsServer1());
            this.networkTable.getItem(5).getItemProperty("Value")
                    .setValue(getNetworkSettingsResponse.getHostDnsServer2());
        } catch (Exception ex) {
            log.error("Failed to get network settings", ex);
        }
    }

    @SuppressWarnings("unchecked")
    public void populateNATTable() {
        try {
            BaseDtoResponse<NATSettingsDto> response = this.getNATSettingsService.dispatch(new Request() {
            });
            this.natTable.getItem(1).getItemProperty("Value").setValue(response.getDto().getPublicIPAddress());
        } catch (Exception ex) {
            log.error("Failed to get NAT settings", ex);
        }
    }
}
