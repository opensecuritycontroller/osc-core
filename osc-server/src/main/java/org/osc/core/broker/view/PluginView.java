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

import org.osc.core.broker.service.api.ImportApplianceManagerPluginServiceApi;
import org.osc.core.broker.service.api.ImportSdnControllerPluginServiceApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.view.common.StyleConstants;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.maintenance.ManagerPluginsLayout;
import org.osc.core.broker.view.maintenance.SdnControllerPluginsLayout;
import org.osc.core.broker.view.util.ViewUtil;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
@Component(service={PluginView.class}, scope=ServiceScope.PROTOTYPE)
public class PluginView extends VerticalLayout implements View {

    private static final String MAINTENANCE_MANAGER_PLUGIN_GUID = "GUID-07FFF1BC-EA9E-426B-A247-4AF6BD12B350.html";
    private static final String MAINTENANCE_CONTROLLER_PLUGIN_GUID = "GUID-DD7DFB29-CB4F-4DD1-BFF5-21694F740D0E.html";

    TabSheet subMenu = null;
    TabSheet tabs = new TabSheet();

    @Reference
    ImportSdnControllerPluginServiceApi importSdnControllerPluginService;

    @Reference
    ImportApplianceManagerPluginServiceApi importApplianceManagerPluginService;

    @Reference
    ServerApi server;

    @Activate
    void start(BundleContext ctx) throws Exception {
        setSizeFull();
        addStyleName(StyleConstants.BASE_CONTAINER);

        // creating tabs sheet for this view
        this.tabs.addStyleName(ValoTheme.TABSHEET_FRAMED);
        this.tabs.setSizeFull();

        // adding SDN Controller Plugin
        this.tabs.addTab(createTab("SDN Controller Plugins", "SDN Controller Plugins",
                new SdnControllerPluginsLayout(ctx, this.importSdnControllerPluginService, this.server),
                MAINTENANCE_CONTROLLER_PLUGIN_GUID));

        // Adding Manager Plugin tab
        this.tabs.addTab(createTab(VmidcMessages.getString(VmidcMessages_.MAINTENANCE_MANAGERPLUGIN_TITLE),
                VmidcMessages.getString(VmidcMessages_.MAINTENANCE_MANAGERPLUGIN_NAME),
                new ManagerPluginsLayout(ctx, this.importApplianceManagerPluginService, this.server),
                MAINTENANCE_MANAGER_PLUGIN_GUID));

        // adding tab sheet to the view
        addComponent(this.tabs);
        setExpandRatio(this.tabs, 1L);
    }

    private VerticalLayout createTab(String caption, String title, FormLayout content, String guid) {
        VerticalLayout tabSheet = new VerticalLayout();
        tabSheet.setCaption(caption);
        tabSheet.setStyleName(StyleConstants.TAB_SHEET);
        Panel panel = new Panel();
        // creating subHeader inside panel
        panel.setContent(content);
        panel.setSizeFull();
        tabSheet.addComponent(ViewUtil.createSubHeader(title, guid));
        tabSheet.addComponent(panel);
        return tabSheet;
    }

    @Override
    public void enter(ViewChangeEvent event) {
    }

}
