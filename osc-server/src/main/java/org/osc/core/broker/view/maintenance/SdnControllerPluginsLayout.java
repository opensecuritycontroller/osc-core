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
import java.io.File;
import java.net.URI;
import java.nio.file.Files;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.plugin.Plugin;
import org.osc.core.broker.model.plugin.Plugin.State;
import org.osc.core.broker.model.plugin.PluginEvent;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.service.ImportSdnControllerPluginService;
import org.osc.core.broker.service.appliance.ImportFileRequest;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.maintenance.PluginUploader.PluginType;
import org.osc.core.broker.view.maintenance.PluginUploader.UploadSucceededListener;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.VmidcWindow;
import org.osc.core.broker.window.WindowUtil;
import org.osc.core.broker.window.button.OkCancelButtonModel;
import org.osc.core.server.installer.InstallableUnit;
import org.osc.sdk.controller.api.SdnControllerApi;
import org.osc.sdk.sdn.api.VMwareSdnApi;

import com.vaadin.data.Item;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FileDownloader;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

public class SdnControllerPluginsLayout extends FormLayout {

    private static final String PROP_PLUGIN_INFO = "Info";
    private static final String PROP_PLUGIN_STATE = "State";
    private static final String PROP_PLUGIN_NAME = "Name";
    private static final String PROP_PLUGIN_SERVICES = "Services";
    private static final String PROP_PLUGIN_VERSION = "Version";
    private static final String PROP_PLUGIN_DELETE = "";

    private static final String OSC_SDN_CONTROLLER_SDK_JAR_PATH = "/SDK/OscSdnControllerPlugin-sources.jar";

    private static final long serialVersionUID = 1L;

    Logger log = Logger.getLogger(SdnControllerPluginsLayout.class);

    private Table plugins;
    private Panel pluginsPanel;
    PluginUploader uploader = new PluginUploader(PluginType.SDN);

    public SdnControllerPluginsLayout() throws Exception {
        super();

        VerticalLayout uploadContainer = new VerticalLayout();
        VerticalLayout pluginsContainer = new VerticalLayout();
        VerticalLayout sdkContainer = new VerticalLayout();

        this.uploader.setSizeFull();
        this.uploader.addSucceededListener(getUploadSucceededListener());

        uploadContainer.addComponent(ViewUtil.createSubHeader("Upload", null));
        uploadContainer.addComponent(this.uploader);

        this.plugins = new Table();
        this.plugins.setPageLength(5);
        this.plugins.setImmediate(true);
        this.plugins.addContainerProperty(PROP_PLUGIN_STATE, String.class, null);
        this.plugins.addContainerProperty(PROP_PLUGIN_NAME, String.class, null);
        this.plugins.addContainerProperty(PROP_PLUGIN_SERVICES, Integer.class, null);
        this.plugins.addContainerProperty(PROP_PLUGIN_VERSION, String.class, null);
        this.plugins.addContainerProperty(PROP_PLUGIN_INFO, String.class, null);
        this.plugins.addContainerProperty(PROP_PLUGIN_DELETE, Button.class, null);

        this.pluginsPanel = new Panel();
        this.pluginsPanel.setContent(this.plugins);

        pluginsContainer.addComponent(ViewUtil.createSubHeader("Plugins", null));
        pluginsContainer.addComponent(this.pluginsPanel);

        Button downloadSdk = new Button(VmidcMessages.getString(VmidcMessages_.MAINTENANCE_SDNPLUGIN_DOWNLOAD_SDK));

        URI currentLocation = UI.getCurrent().getPage().getLocation();
        FileDownloader downloader = new FileDownloader(
                new ExternalResource("https://" + currentLocation.getHost() + OSC_SDN_CONTROLLER_SDK_JAR_PATH));

        downloader.extend(downloadSdk);

        Panel sdkLinkPanel = new Panel();
        sdkLinkPanel.setContent(downloadSdk);

        sdkContainer.addComponent(ViewUtil.createSubHeader("SDK", null));
        sdkContainer.addComponent(sdkLinkPanel);

        addComponent(uploadContainer);
        addComponent(pluginsContainer);
        addComponent(sdkContainer);


        // Subscribe to Plugin Notifications
        SdnControllerApiFactory.<SdnControllerApi>newPluginTracker(ev -> {
            updateTable(ev);
        }, SdnControllerApi.class, PluginType.SDN);

        SdnControllerApiFactory.<VMwareSdnApi>newPluginTracker(ev -> {
            updateTable(ev);
        }, VMwareSdnApi.class, PluginType.NSX);
    }

    private void updateTable(PluginEvent<?> event) {
        Plugin<?> plugin = event.getPlugin();
        switch (event.getType()) {
        case ADDING:
            Item addingItem = this.plugins.addItem(plugin);
            if (addingItem != null) {
                updateItem(addingItem, plugin);
            }
            break;
        case MODIFIED:
            Item modifyingItem = this.plugins.getItem(plugin);
            if (modifyingItem == null) {
                modifyingItem = this.plugins.addItem(plugin);
            }
            if (modifyingItem != null) {
                updateItem(modifyingItem, plugin);
            }
            break;
        case REMOVED:
            this.plugins.removeItem(plugin);
            break;
        default:
        	this.log.error("Unknown plugin event type: " + event.getType());
        	break;
        }
    }

    @SuppressWarnings("unchecked")
    private void updateItem(Item item, Plugin<?> plugin) {
        InstallableUnit installUnit = plugin.getInstallUnit();

        item.getItemProperty(PROP_PLUGIN_STATE).setValue(plugin.getState().toString());
        item.getItemProperty(PROP_PLUGIN_NAME).setValue(installUnit.getSymbolicName());
        item.getItemProperty(PROP_PLUGIN_VERSION).setValue(installUnit.getVersion());
        item.getItemProperty(PROP_PLUGIN_SERVICES).setValue(plugin.getServices() != null ? plugin.getServices().size() : 0);

        String info;
        if (plugin.getState() == State.ERROR) {
            info = plugin.getError();
        } else {
            info = "";
        }
        item.getItemProperty(PROP_PLUGIN_INFO).setValue(info);

        Button deleteButton = new Button("Delete");
        deleteButton.addClickListener(event -> deletePlugin(installUnit));
        item.getItemProperty(PROP_PLUGIN_DELETE).setValue(deleteButton);
    }

    private void deletePlugin(InstallableUnit installUnit) {
        final VmidcWindow<OkCancelButtonModel> deleteWindow = WindowUtil.createAlertWindow("Delete Plugin", "Delete Plugin - " + installUnit.getSymbolicName());
        deleteWindow.getComponentModel().setOkClickedListener(event -> {
            if (VirtualizationConnectorEntityMgr.isControllerTypeUsed(installUnit.getName())) {
                ViewUtil.iscNotification("SDN Controller Plugin '" + installUnit.getName() + "' is used.", Notification.Type.ERROR_MESSAGE);
            } else {
                try {
                    File origin = installUnit.getOrigin();
                    if (origin == null) {
                        throw new IllegalStateException(String.format("Install unit %s has no origin file.", installUnit.getSymbolicName()));
                    }

                    // Use Java 7 Files.delete(), as it throws an informative exception when deletion fails
                    Files.delete(origin.toPath());
                } catch (Exception e) {
                    ViewUtil.showError("Fail to unload Manager Plugin '" + installUnit.getSymbolicName() + "'", e);
                }
            }
            deleteWindow.close();
        });
        ViewUtil.addWindow(deleteWindow);
    }

    private UploadSucceededListener getUploadSucceededListener() {
        return new UploadSucceededListener() {

            @Override
            public void uploadComplete(String uploadPath) {
                try {
                    ImportFileRequest importRequest = new ImportFileRequest(uploadPath);

                    ImportSdnControllerPluginService service = new ImportSdnControllerPluginService();
                    service.dispatch(importRequest);

                    ViewUtil.iscNotification(
                            VmidcMessages.getString(VmidcMessages_.UPLOAD_PLUGIN_SDNCONTROLLER_SUCCESSFUL), null,
                            Notification.Type.TRAY_NOTIFICATION);
                } catch (Exception e) {
                    SdnControllerPluginsLayout.this.log.info(e.getMessage());
                    ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
                }
            }

        };
    }

}
