/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.osc.core.broker.service.BackupService;
import org.osc.core.broker.service.request.BackupRequest;
import org.osc.core.broker.service.response.BackupResponse;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.util.ArchiveUtil;
import org.osc.core.util.NetworkUtil;
import org.osc.core.util.ServerUtil;
import org.osc.core.util.VersionUtil;

import com.mcafee.vmidc.server.Server;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

public class SummaryLayout extends FormLayout {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private Table summarytable = null;
    private CheckBox checkbox = null;
    private Button download = null;
    //private boolean logBundle = true;

    private static final Logger log = Logger.getLogger(SummaryLayout.class);

    public SummaryLayout() {
        super();
        this.summarytable = createTable();
        // creating Server table
        this.summarytable.addItem(new Object[] { "DNS Name: ", getHostName() }, new Integer(1));
        this.summarytable.addItem(new Object[] { "IP Address: ", getIpAddress() }, new Integer(2));
        this.summarytable.addItem(new Object[] { "Version: ", getVersion() }, new Integer(3));
        this.summarytable.addItem(new Object[] { "Uptime: ", ServerUtil.uptimeToString() }, new Integer(4));
        this.summarytable.addItem(new Object[] { "Current Server Time: ", new Date().toString() }, new Integer(5));

        VerticalLayout tableContainer = new VerticalLayout();
        tableContainer.addComponent(this.summarytable);
        addComponent(tableContainer);
        addComponent(createCheckBox());

        HorizontalLayout actionContainer = new HorizontalLayout();
        actionContainer.addComponent(createDownloadButton());
        if (Server.devMode) {
            actionContainer.addComponent(createShutdownButton());
        }
        actionContainer.addComponent(createRestartButton());
        addComponent(actionContainer);
    }

    @SuppressWarnings("serial")
    private Component createShutdownButton() {
        Button shutdown = new Button(VmidcMessages.getString(VmidcMessages_.SUMMARY_SHUTDOWN));

        shutdown.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                Thread shutdownThread = new Thread("Stop-Server-Thread") {
                    @Override
                    public void run() {
                        Server.stopServer();
                    }
                };
                shutdownThread.start();
            }
        });

        return shutdown;
    }

    @SuppressWarnings("serial")
    private Component createRestartButton() {
        Button shutdown = new Button(VmidcMessages.getString(VmidcMessages_.SUMMARY_RESTART));

        shutdown.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                ViewUtil.iscNotification(VmidcMessages.getString(VmidcMessages_.SUMMARY_RESTART_STARTED), null,
                        Notification.Type.TRAY_NOTIFICATION);
                ViewUtil.showServerRestartProgress();
                Server.restart();
            }
        });

        return shutdown;
    }

    private Table createTable() {
        Table table = new Table();
        table.setSizeFull();
        table.setPageLength(0);
        table.setSelectable(false);
        table.setColumnCollapsingAllowed(true);
        table.setColumnReorderingAllowed(true);
        table.setImmediate(true);
        table.setNullSelectionAllowed(false);
        table.addContainerProperty("Name", String.class, null);
        table.addContainerProperty("Status", String.class, null);
        table.setColumnHeaderMode(Table.ColumnHeaderMode.HIDDEN);
        return table;
    }

    @SuppressWarnings("serial")
    private Button createDownloadButton() {
        this.download = new Button(VmidcMessages.getString(VmidcMessages_.SUMMARY_DOWNLOAD_LOG)) {
            @Override
            public void setEnabled(boolean enabled) {
                if (enabled) {
                    try {
                        // because setEnabled(false) calls are ignored and button is disabled
                        // on client because of setDisableOnClick(true), by doing this we
                        // make sure that the button is actually disabled so that setEnabled(true)
                        // has effect
                        getUI().getConnectorTracker().getDiffState(this).put("enabled", false);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    super.setEnabled(enabled);
                }
            }
        };
        SummaryLayout.this.download.setDisableOnClick(true);
        if (this.checkbox != null && this.checkbox.getValue()) {
            this.download.setCaption(VmidcMessages.getString(VmidcMessages_.SUMMARY_DOWNLOAD_BUNDLE));
        }
        StreamResource zipStream = getZipStream();
        FileDownloader fileDownloader = new FileDownloader(zipStream);
        fileDownloader.extend(this.download);
        return this.download;
    }

    @SuppressWarnings("serial")
    private CheckBox createCheckBox() {
        this.checkbox = new CheckBox(VmidcMessages.getString(VmidcMessages_.SUMMARY_DOWNLOAD_INCLUDE_DB));
        this.checkbox.setImmediate(true);
        this.checkbox.setValue(false);
        this.checkbox.addValueChangeListener(new ValueChangeListener() {

            @Override
            public void valueChange(ValueChangeEvent event) {
                if (SummaryLayout.this.checkbox.getValue()) {
                    SummaryLayout.this.download
                            .setCaption(VmidcMessages.getString(VmidcMessages_.SUMMARY_DOWNLOAD_BUNDLE));
                } else {
                    SummaryLayout.this.download
                            .setCaption(VmidcMessages.getString(VmidcMessages_.SUMMARY_DOWNLOAD_LOG));
                }
            }
        });
        return this.checkbox;
    }

    @SuppressWarnings("serial")
    private StreamResource getZipStream() {
        StreamResource.StreamSource source = new StreamResource.StreamSource() {
            @Override
            public InputStream getStream() {
                InputStream fin = null;
                try {
                    if (SummaryLayout.this.checkbox.getValue()) {
                        getDBBackup();
                    }
                    // creating a zip file resource to download
                    fin = new FileInputStream(ArchiveUtil.archive("log", "ServerSupportBundle.zip"));
                } catch (Exception exception) {
                    log.error("Failed! to receive zip file from Archieve Util", exception);
                } finally {
                	new BackupService().deleteBackupFilesFrom("log");
                    SummaryLayout.this.download.setEnabled(true);
                }
                return fin;
            }
        };
        StreamResource resource = new StreamResource(source, "ServerSupportBundle.zip");
        return resource;
    }

    private void getDBBackup() {
        try {
            BackupService bkpservice = new BackupService();
            BackupResponse res = bkpservice.dispatch(new BackupRequest());
            if (res.isSuccess()) {
            	// move backup to log directory
                FileUtils.copyFileToDirectory(bkpservice.getEncryptedBackupFile(), new File("log" + File.separator));
                bkpservice.deleteBackupFiles();
                log.info("Backup Successful! adding backup file to Support Bundle");
            }
        } catch (Exception e) {
            log.error("Failed to add DB backup in support bundle", e);
        }

    }

    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            log.error("Error while reading Host Name ", e);
        }
        return "";
    }

    public String getIpAddress() {
        try {
            return NetworkUtil.getHostIpAddress();
        } catch (Exception e) {
            log.error("Error while Host IP address ", e);
        }
        return "";
    }

    private String getVersion() {
        return VersionUtil.getVersion().getVersionStr();
    }

}
