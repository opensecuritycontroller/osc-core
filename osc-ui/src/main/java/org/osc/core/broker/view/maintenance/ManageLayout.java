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

import org.osc.core.broker.service.api.BackupServiceApi;
import org.osc.core.broker.service.api.RestoreServiceApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.api.server.ValidationApi;
import org.osc.core.broker.service.request.BackupRequest;
import org.osc.core.broker.service.response.BackupResponse;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.add.PasswordWindow;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.vaadin.server.FileResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Link;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

public class ManageLayout extends FormLayout {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(ManageLayout.class);

    private Button backupButton = null;
    private Link downloadBackup = null;
    private HorizontalLayout linkContainer = null;

    private final BackupServiceApi backupService;

    private final ValidationApi validator;

    public ManageLayout(BackupServiceApi backupService, RestoreServiceApi restoreService,
            ServerApi server, ValidationApi validator) {
        super();
        this.backupService = backupService;
        this.validator = validator;

        VerticalLayout backupContainer = new VerticalLayout();
        VerticalLayout restoreContainer = new VerticalLayout();

        DbRestorer restorer = new DbRestorer(restoreService, server, validator);
        restorer.setSizeFull();

        // Component to Backup Database
        Panel bkpPanel = new Panel();
        bkpPanel.setContent(createBackup());

        backupContainer.addComponent(ViewUtil.createSubHeader("Backup Database", null));
        backupContainer.addComponent(bkpPanel);

        restoreContainer.addComponent(ViewUtil.createSubHeader("Restore Database", null));
        restoreContainer.addComponent(restorer);

        addComponent(backupContainer);
        addComponent(restoreContainer);
    }

    @SuppressWarnings("serial")
    public VerticalLayout createBackup() {
        final VerticalLayout bkpLayout = new VerticalLayout();
        bkpLayout.addStyleName("componentSpacing");
        this.backupButton = new Button("Create Backup");
        this.backupButton.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                try {
                	PasswordWindow passwordWindow = new PasswordWindow(ManageLayout.this.validator);
                	passwordWindow.setSubmitFormListener(password -> {
                		try {
							BackupRequest req = new BackupRequest();
		                    req.setBackupPassword(password);
		                    BackupResponse res = ManageLayout.this.backupService.dispatch(req);
		                    if (res.isSuccess()) {
		                        ViewUtil.iscNotification("Backup Successful!", null, Notification.Type.TRAY_NOTIFICATION);
		                        ManageLayout.this.linkContainer.removeAllComponents();
		                        createDownloadLink(ManageLayout.this.backupService.getEncryptedBackupFile());
		                    }
						} catch (Exception e) {
		                    ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
		                    log.error("Failed to backup vmiDCServer Database ", e);
		                }
                	});

                	ViewUtil.addWindow(passwordWindow);
                } catch (Exception e) {
                    ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
                    log.error("Failed to backup vmiDCServer Database ", e);
                }
            }

        });

        bkpLayout.addComponent(this.backupButton);
        this.linkContainer = new HorizontalLayout();
        File backupFile = this.backupService.getEncryptedBackupFile();
        if (backupFile.exists()) {
            createDownloadLink(backupFile);
        }
        bkpLayout.addComponent(this.linkContainer);
        return bkpLayout;
    }

    private void createDownloadLink(File backupFile) {
        this.downloadBackup = new Link("Download Backup: " + backupFile.getName(), new FileResource(backupFile));
        this.linkContainer.addComponent(this.downloadBackup);
    }

}
