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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.UpgradeServiceApi;
import org.osc.core.broker.service.request.UpgradeRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;
import org.osc.core.broker.view.common.StyleConstants;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.ProgressIndicatorWindow;
import org.osc.core.broker.window.UploadInfoWindow;

import com.vaadin.server.communication.FileUploadHandler.UploadInterruptedException;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.FailedEvent;
import com.vaadin.ui.Upload.FailedListener;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.StartedEvent;
import com.vaadin.ui.Upload.StartedListener;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;
import com.vaadin.ui.VerticalLayout;

public class Upgrader extends CustomComponent implements Receiver, FailedListener, SucceededListener {
    private static final Logger log = Logger.getLogger(Upgrader.class);

    public static final String UPLOAD_DIR = "/tmp/";
    private static final long serialVersionUID = 1L;

    private Upload upload;
    private File file;
    private final Panel panel = new Panel();
    private final VerticalLayout verLayout = new VerticalLayout();

    private UpgradeServiceApi upgradeService;

    public Upgrader(UpgradeServiceApi upgradeService) {
        this.upgradeService = upgradeService;
        // Create vmidc upload folder
        File uploadFolder = new File(Upgrader.UPLOAD_DIR);
        if (!uploadFolder.exists() && !uploadFolder.mkdir()) {
            log.error("Error creating upload folder");
        }
        createUpload();
        this.verLayout.setSpacing(true);
        this.panel.setWidth("100%");
        this.panel.setContent(this.verLayout);

        this.verLayout.addComponent(this.upload);
        this.verLayout.addStyleName(StyleConstants.COMPONENT_SPACING);
        setCompositionRoot(this.panel);
    }

    @Override
    public OutputStream receiveUpload(String filename, String mimeType) {
        if (filename != null && !filename.isEmpty()) {
            log.info("Start uploading file: " + filename);

            try {
                this.file = new File(UPLOAD_DIR + filename);
                return new FileOutputStream(this.file);
            } catch (final java.io.FileNotFoundException e) {
                log.error("Error opening file: " + filename, e);
                ViewUtil.iscNotification(VmidcMessages.getString(VmidcMessages_.UPLOAD_COMMON_ERROR) + filename,
                        Notification.Type.ERROR_MESSAGE);
            }
        }
        return null;
    }

    private EmptySuccessResponse invokeUpgradeService(File file) throws Exception {
        UpgradeRequest req = new UpgradeRequest();
        EmptySuccessResponse res;
        req.setUploadedFile(file);
        res = this.upgradeService.dispatch(req);
        return res;
    }

    @SuppressWarnings("serial")
    private void createUpload() {
        this.upload = new Upload();
        this.upload.setButtonCaption(VmidcMessages.getString("upload.upgrade"));
        this.upload.setReceiver(this);
        this.upload.addFailedListener(this);
        this.upload.addSucceededListener(this);
        this.upload.setImmediate(false);

        final UploadInfoWindow uploadInfoWindow = new UploadInfoWindow(this.upload);

        this.upload.addStartedListener(new StartedListener() {
            @Override
            public void uploadStarted(final StartedEvent event) {
                if (uploadInfoWindow.getParent() == null) {
                    ViewUtil.addWindow(uploadInfoWindow);
                }
            }
        });

    }

    private void repaintUpload() {
        boolean enabled = this.upload.isEnabled();
        this.verLayout.removeComponent(this.upload);
        createUpload();
        this.upload.setEnabled(enabled);
        this.verLayout.addComponent(this.upload);
    }

    @Override
    public void uploadSucceeded(SucceededEvent event) {
        final ProgressIndicatorWindow progressIndicatorWindow = new ProgressIndicatorWindow();
        try {
            log.info("================ File upload completed");
            log.info("================ Upgrade process starting...");

            // add modal window on screen when Upgrading...
            progressIndicatorWindow.setWidth("550px");
            progressIndicatorWindow.setHeight("130px");
            progressIndicatorWindow.setCaption(VmidcMessages.getString(VmidcMessages_.UPLOAD_UPGRADE_UPGRADING));
            progressIndicatorWindow.updateStatus(VmidcMessages.getString(VmidcMessages_.UPLOAD_UPGRADE_NOTE));

            UI.getCurrent().addWindow(progressIndicatorWindow);
            progressIndicatorWindow.bringToFront();

            invokeUpgradeService(this.file);
            ViewUtil.iscNotification(VmidcMessages.getString(VmidcMessages_.UPLOAD_UPGRADE_SUCCESSFUL, new Date()),
                    null, Notification.Type.TRAY_NOTIFICATION);
            log.info("=============== Upgrade succeeded");
        } catch (SQLException ex) {
            log.error(
                    "=============== Failed to upgrade with SQL exception. Ignoring since upgrade does not rely on SQL. "
                            + "Assuming all other upgrade actions succeeded", ex);
            ViewUtil.iscNotification(VmidcMessages.getString(VmidcMessages_.UPLOAD_UPGRADE_SUCCESSFUL, new Date()),
                    null, Notification.Type.TRAY_NOTIFICATION);
        } catch (Exception ex) {
            log.error("=============== Failed to upgrade", ex);
            ViewUtil.iscNotification("Upgrade failed. " + ex.getMessage() + " Please use a valid zip file.",
                    Notification.Type.ERROR_MESSAGE);
            repaintUpload();
            progressIndicatorWindow.close();
        }
    }

    @Override
    public void uploadFailed(FailedEvent event) {
        log.error(new Label(new Date() + ": Upload failed."));
        if (event.getFilename() == null || event.getFilename().isEmpty()) {
            log.error("No upload file specified");
            ViewUtil.iscNotification(VmidcMessages.getString(VmidcMessages_.UPLOAD_UPGRADE_NOFILE),
                    Notification.Type.ERROR_MESSAGE);
            repaintUpload();
        } else if (event.getReason() instanceof UploadInterruptedException) {
            log.warn("Server Upgrade Bundle upload is cancelled by the user");

        } else {
            log.error("Upload failed");
            ViewUtil.iscNotification(VmidcMessages.getString(VmidcMessages_.UPLOAD_UPGRADE_FAILED),
                    Notification.Type.ERROR_MESSAGE);
            repaintUpload();
        }
        if (this.file != null && this.file.exists()) {
            try {
                FileUtils.forceDelete(this.file);
            } catch (IOException e) {
                log.error("Deleting upload file: " + this.file + " failed when upload was cancelled by user.", e);
            }
        }
    }
}