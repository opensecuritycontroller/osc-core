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
import java.util.Date;

import org.osc.core.broker.service.api.ArchiveServiceApi;
import org.osc.core.broker.service.api.GetJobsArchiveServiceApi;
import org.osc.core.broker.service.api.UpdateJobsArchiveServiceApi;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.VmidcWindow;
import org.osc.core.broker.window.WindowUtil;
import org.osc.core.broker.window.button.OkCancelButtonModel;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.vaadin.server.FileResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Link;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

public class ArchiveLayout extends FormLayout {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    Logger log = LoggerFactory.getLogger(ArchiveLayout.class);

    private Table archiveTable;

    public ArchiveLayout(ArchiveServiceApi archiveService, GetJobsArchiveServiceApi getJobsArchiveService,
            UpdateJobsArchiveServiceApi updateJobsArchiveService) {
        super();

        VerticalLayout downloadContainer = new VerticalLayout();
        VerticalLayout archiveContainer = new VerticalLayout();

        // Component to Archive Jobs
        JobsArchiverPanel archiveConfigurator = new JobsArchiverPanel(this, archiveService,
                getJobsArchiveService, updateJobsArchiveService);
        archiveConfigurator.setSizeFull();

        archiveContainer.addComponent(ViewUtil.createSubHeader("Archive Jobs/Alerts", null));
        archiveContainer.addComponent(archiveConfigurator);

        downloadContainer.addComponent(ViewUtil.createSubHeader("Download Archive", null));
        // Component to download archive

        this.archiveTable = new Table();
        this.archiveTable.setSizeFull();
        this.archiveTable.setPageLength(5);
        this.archiveTable.setImmediate(true);
        this.archiveTable.addContainerProperty("Name", String.class, null);
        this.archiveTable.addContainerProperty("Date", Date.class, null);
        this.archiveTable.addContainerProperty("Size", Long.class, null);
        this.archiveTable.addContainerProperty("Download", Link.class, null);
        this.archiveTable.addContainerProperty("Delete", Button.class, null);
        buildArchivesTable();

        Panel archiveTablePanel = new Panel();
        archiveTablePanel.setContent(this.archiveTable);

        addComponent(archiveContainer);
        addComponent(archiveTablePanel);
    }

    public void buildArchivesTable() {
        this.archiveTable.removeAllItems();

        File[] fileList = new File("archive").listFiles();

        if(fileList == null) {
            fileList = new File[0];
        }

        for (File archiveFile : fileList) {
            this.archiveTable.addItem(new Object[] { archiveFile.getName(), new Date(archiveFile.lastModified()),
                    archiveFile.length(), createDownloadLink(archiveFile), createDeleteArchive(archiveFile) },
                    archiveFile.getName());
        }
        this.archiveTable.sort(new Object[] { "Date" }, new boolean[] { false });
    }

    @SuppressWarnings("serial")
    private Button createDeleteArchive(File archiveFile) {
        final Button deleteArchiveButton = new Button("Delete");
        deleteArchiveButton.setData(archiveFile);
        deleteArchiveButton.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                final File archiveFile = (File) deleteArchiveButton.getData();
                final VmidcWindow<OkCancelButtonModel> deleteWindow = WindowUtil.createAlertWindow(
                        "Delete Archive File", "Delete Archive File - " + archiveFile);
                deleteWindow.getComponentModel().setOkClickedListener(new ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        archiveFile.delete();
                        buildArchivesTable();
                        deleteWindow.close();
                    }
                });
                ViewUtil.addWindow(deleteWindow);
            }
        });
        return deleteArchiveButton;
    }

    private Link createDownloadLink(File archiveFile) {
        return new Link("Download " + archiveFile, new FileResource(archiveFile));
    }


}
