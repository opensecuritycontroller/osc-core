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

import org.apache.log4j.Logger;
import org.osc.core.broker.service.BackupService;
import org.osc.core.broker.service.request.BackupRequest;
import org.osc.core.broker.service.response.BackupResponse;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.add.PasswordWindow;

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

    private static Logger log = Logger.getLogger(ManageLayout.class);

    private Button backupButton = null;
    private Link downloadBackup = null;
    private HorizontalLayout linkContainer = null;

    public ManageLayout() {
        super();

        VerticalLayout upgradeContainer = new VerticalLayout();
        VerticalLayout backupContainer = new VerticalLayout();
        VerticalLayout restoreContainer = new VerticalLayout();

        // Component to Upgrade Server
        Upgrader upgrader = new Upgrader();
        upgrader.setSizeFull();

        DbRestorer restorer = new DbRestorer();
        restorer.setSizeFull();

        // Component to Backup Database
        Panel bkpPanel = new Panel();
        bkpPanel.setContent(createBackup());

        // we do not want help button for these sub sub headers
        upgradeContainer.addComponent(ViewUtil.createSubHeader("Upgrade", null));
        upgradeContainer.addComponent(upgrader);

        backupContainer.addComponent(ViewUtil.createSubHeader("Backup Database", null));
        backupContainer.addComponent(bkpPanel);

        restoreContainer.addComponent(ViewUtil.createSubHeader("Restore Database", null));
        restoreContainer.addComponent(restorer);

        addComponent(upgradeContainer);
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
                	PasswordWindow passwordWindow = new PasswordWindow();
                	passwordWindow.setSubmitFormListener(password -> {
                		try {
							BackupRequest req = new BackupRequest();
		                    req.setBackupPassword(password);
		                    BackupService backupService = new BackupService();
		                    BackupResponse res = backupService.dispatch(req);
		                    if (res.isSuccess()) {
		                        ViewUtil.iscNotification("Backup Successful!", null, Notification.Type.TRAY_NOTIFICATION);
		                        ManageLayout.this.linkContainer.removeAllComponents();
		                        createDownloadLink(backupService.getEncryptedBackupFile());
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
        BackupService backupService = new BackupService();
        File backupFile = backupService.getEncryptedBackupFile();
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
