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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.osc.core.broker.service.api.ImportPluginServiceApi;
import org.osc.core.broker.service.api.plugin.PluginApi;
import org.osc.core.broker.service.api.plugin.PluginApi.State;
import org.osc.core.broker.service.api.plugin.PluginEvent;
import org.osc.core.broker.service.api.plugin.PluginListener;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.request.ImportFileRequest;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.maintenance.PluginUploader.UploadSucceededListener;
import org.osc.core.broker.view.util.SdkUtil;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.VmidcWindow;
import org.osc.core.broker.window.WindowUtil;
import org.osc.core.broker.window.button.OkCancelButtonModel;
import org.osc.core.ui.LogProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;

import com.vaadin.data.Item;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FileDownloader;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

public class PluginsLayout extends FormLayout {

    private static final String PROP_PLUGIN_INFO = "Info";
    private static final String PROP_PLUGIN_STATE = "State";
    private static final String PROP_PLUGIN_NAME = "Name";
    private static final String PROP_PLUGIN_SERVICES = "Services";
    private static final String PROP_PLUGIN_VERSION = "Version";
    private static final String PROP_PLUGIN_DELETE = "";

    private static final long serialVersionUID = 1L;

    Logger log = LogProvider.getLogger(PluginsLayout.class);

    private Table plugins;
    private Panel pluginsPanel;
    PluginUploader uploader;

    private ImportPluginServiceApi importPluginService;
    private ServiceRegistration<PluginListener> registration;

    public PluginsLayout(BundleContext ctx, ImportPluginServiceApi importPluginService,
            ServerApi server) throws Exception {
        super();
        this.importPluginService = importPluginService;

        this.uploader = new PluginUploader(server);
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

        Button downloadSdkSdn = getDownloadSdkButtonForSdnController();
        Button downloadSdkManager = getDownloadSdkButtonForManager();
        
        sdkContainer.addComponent(ViewUtil.createSubHeader("SDK", null));
        sdkContainer.addComponent(new HorizontalLayout(downloadSdkSdn, downloadSdkManager));

        addComponent(uploadContainer);
        addComponent(pluginsContainer);
        addComponent(sdkContainer);


        // Subscribe to Plugin Notifications
        this.registration = ctx.registerService(PluginListener.class,
                this::updateTable, null);
    }

    private void updateTable(PluginEvent event) {
        PluginApi plugin = event.getPlugin();
        
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

    private Button getDownloadSdkButtonForSdnController() throws URISyntaxException, MalformedURLException {
        SdkUtil sdkUtil = new SdkUtil();
        Button downloadSdk = new Button(VmidcMessages.getString(VmidcMessages_.MAINTENANCE_SDNPLUGIN_DOWNLOAD_SDK));
        URI currentLocation = UI.getCurrent().getPage().getLocation();
        URI downloadLocation = new URI(currentLocation.getScheme(), null, currentLocation.getHost(),
                currentLocation.getPort(), sdkUtil.getSdk(SdkUtil.sdkType.SDN_CONTROLLER), null, null);
        FileDownloader downloader = new FileDownloader(new ExternalResource(downloadLocation.toURL().toString()));
        downloader.extend(downloadSdk);
        return downloadSdk;
    }

    private Button getDownloadSdkButtonForManager() throws URISyntaxException, MalformedURLException {
        SdkUtil sdkUtil = new SdkUtil();
        Button downloadSdk = new Button(VmidcMessages.getString(VmidcMessages_.MAINTENANCE_MANAGERPLUGIN_DOWNLOAD_SDK));
        URI currentLocation = UI.getCurrent().getPage().getLocation();
        URI downloadLocation = new URI(currentLocation.getScheme(), null, currentLocation.getHost(),
                currentLocation.getPort(), sdkUtil.getSdk(SdkUtil.sdkType.MANAGER), null, null);
        FileDownloader downloader = new FileDownloader(new ExternalResource(downloadLocation.toURL().toString()));
        downloader.extend(downloadSdk);
        return downloadSdk;
    }
    
    @SuppressWarnings("unchecked")
    private void updateItem(Item item, PluginApi plugin) {

        item.getItemProperty(PROP_PLUGIN_STATE).setValue(plugin.getState().toString());
        item.getItemProperty(PROP_PLUGIN_NAME).setValue(plugin.getSymbolicName());
        item.getItemProperty(PROP_PLUGIN_VERSION).setValue(plugin.getVersion());
        item.getItemProperty(PROP_PLUGIN_SERVICES).setValue(plugin.getServiceCount());

        String info;
        if (plugin.getState() == State.ERROR) {
            info = plugin.getError();
        } else {
            info = "";
        }
        item.getItemProperty(PROP_PLUGIN_INFO).setValue(info);

        Button deleteButton = new Button("Delete");
        deleteButton.addClickListener(event -> deletePlugin(plugin));
        item.getItemProperty(PROP_PLUGIN_DELETE).setValue(deleteButton);
    }

    private void deletePlugin(PluginApi plugin) {
        final VmidcWindow<OkCancelButtonModel> deleteWindow = WindowUtil.createAlertWindow("Delete Plugin", "Delete Plugin - " + plugin.getSymbolicName());
        deleteWindow.getComponentModel().setOkClickedListener(event -> {
            if (this.importPluginService.isControllerTypeUsed(plugin.getName())) {
                ViewUtil.iscNotification("SDN Controller Plugin '" + plugin.getName() + "' is used.", Notification.Type.ERROR_MESSAGE);
            } else {
                try {
                    File origin = plugin.getOrigin();
                    if (origin == null) {
                        throw new IllegalStateException(String.format("Install unit %s has no origin file.", plugin.getSymbolicName()));
                    }

                    // Use Java 7 Files.delete(), as it throws an informative exception when deletion fails
                    Files.delete(origin.toPath());
                } catch (Exception e) {
                    ViewUtil.showError("Fail to remove Plugin '" + plugin.getSymbolicName() + "'", e);
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

                    PluginsLayout.this.importPluginService.dispatch(importRequest);

                    ViewUtil.iscNotification(
                            VmidcMessages.getString(VmidcMessages_.UPLOAD_PLUGIN_SUCCESSFUL), null,
                            Notification.Type.TRAY_NOTIFICATION);
                } catch (Exception e) {
                    PluginsLayout.this.log.info(e.getMessage());
                    ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
                }
            }

        };
    }

    @Override
    public void detach() {
        this.registration.unregister();
        super.detach();
    }
}
