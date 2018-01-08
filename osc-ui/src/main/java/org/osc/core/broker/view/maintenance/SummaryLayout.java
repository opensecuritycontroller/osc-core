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
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.osc.core.broker.service.api.BackupServiceApi;
import org.osc.core.broker.service.api.server.ArchiveApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.request.BackupRequest;
import org.osc.core.broker.service.response.BackupResponse;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
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

    private ServerApi server;

    private BackupServiceApi backupService;

    private ArchiveApi archiver;

    private static final Logger log = LoggerFactory.getLogger(SummaryLayout.class);


    public SummaryLayout(ServerApi server, BackupServiceApi backupService,
            ArchiveApi archiver) {
        super();
        this.server = server;
        this.backupService = backupService;
        this.archiver = archiver;
        this.summarytable = createTable();
        // creating Server table
        this.summarytable.addItem(new Object[] { "DNS Name: ", getHostName() }, new Integer(1));
        this.summarytable.addItem(new Object[] { "IP Address: ", getIpAddress() }, new Integer(2));
        this.summarytable.addItem(new Object[] { "Version: ", getVersion() }, new Integer(3));
        this.summarytable.addItem(new Object[] { "Uptime: ", server.uptimeToString() }, new Integer(4));
        this.summarytable.addItem(new Object[] { "Current Server Time: ", new Date().toString() }, new Integer(5));

        VerticalLayout tableContainer = new VerticalLayout();
        tableContainer.addComponent(this.summarytable);
        addComponent(tableContainer);
        addComponent(createCheckBox());

        HorizontalLayout actionContainer = new HorizontalLayout();
        actionContainer.addComponent(createDownloadButton());
        addComponent(actionContainer);
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
                    // because setEnabled(false) calls are ignored and button is disabled
                    // on client because of setDisableOnClick(true), by doing this we
                    // make sure that the button is actually disabled so that setEnabled(true)
                    // has effect
                    getUI().getConnectorTracker().getDiffState(this).put("enabled", false);
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
                    fin = new FileInputStream(SummaryLayout.this.archiver.archive("log", "ServerSupportBundle.zip"));
                } catch (Exception exception) {
                    log.error("Failed! to receive zip file from Archieve Util", exception);
                } finally {
                	SummaryLayout.this.backupService.deleteBackupFilesFrom("log");
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
            BackupResponse res = this.backupService.dispatch(new BackupRequest());
            if (res.isSuccess()) {
            	// move backup to log directory
                FileUtils.copyFileToDirectory(this.backupService.getEncryptedBackupFile(), new File("log" + File.separator));
                this.backupService.deleteBackupFiles();
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
            return this.server.getHostIpAddress();
        } catch (Exception e) {
            log.error("Error while Host IP address ", e);
        }
        return "";
    }

    private String getVersion() {
        return this.server.getVersionStr();
    }

}
