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

import static org.osc.core.broker.view.common.VmidcMessages.getString;
import static org.osc.core.broker.view.common.VmidcMessages_.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.osc.core.broker.service.ssl.X509TrustManagerApi;
import org.osc.core.broker.view.common.StyleConstants;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.UploadInfoWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.communication.FileUploadHandler.UploadInterruptedException;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.FailedEvent;
import com.vaadin.ui.Upload.FailedListener;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.StartedListener;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;
import com.vaadin.ui.VerticalLayout;

public class SslCertificateUploader extends CustomComponent implements Receiver, FailedListener, SucceededListener {
    private static final Logger log = LoggerFactory.getLogger(SslCertificateUploader.class);

    private static final long serialVersionUID = 1L;
    private File file;
    private UploadNotifier uploadNotifier;

    protected Upload upload;
    protected final VerticalLayout verLayout = new VerticalLayout();

    protected X509TrustManagerApi x509TrustManager;

    public SslCertificateUploader(X509TrustManagerApi x509TrustManager) {
        this.x509TrustManager = x509TrustManager;

        Panel panel = new Panel();
        layout(panel);
        setCompositionRoot(panel);
    }

    protected void layout(Panel panel) {
        createUpload();
        this.verLayout.setSpacing(true);

        panel.setWidth("100%");
        panel.setContent(this.verLayout);

        this.verLayout.addComponent(this.upload);
        this.verLayout.addStyleName(StyleConstants.COMPONENT_SPACING);
    }

    public void setUploadNotifier(UploadNotifier uploadNotifier) {
        this.uploadNotifier = uploadNotifier;
    }

    @Override
    public OutputStream receiveUpload(String filename, String mimeType) {
        if (filename != null && !filename.isEmpty()) {
            log.info("Start uploading certificate: " + filename);

            try {
                this.file = File.createTempFile("tmp", filename);
                return new FileOutputStream(this.file);
            } catch (final java.io.IOException e) {
                log.error("Error opening certificate: " + filename, e);
                ViewUtil.iscNotification(getString(UPLOAD_COMMON_ERROR) + filename,
                        Notification.Type.ERROR_MESSAGE);
            }
        }
        return null;
    }

    protected void createUpload() {
        this.upload = new Upload();
        this.upload.setButtonCaption(getString(MAINTENANCE_SSLCONFIGURATION_UPLOAD));
        this.upload.setReceiver(this);
        this.upload.addFailedListener(this);
        this.upload.addSucceededListener(this);
        this.upload.setImmediate(false);

        final UploadInfoWindow uploadInfoWindow = new UploadInfoWindow(this.upload);

        this.upload.addStartedListener((StartedListener) event -> {
            if (uploadInfoWindow.getParent() == null) {
                ViewUtil.addWindow(uploadInfoWindow);
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
        boolean succeeded = true;
        try {
            processCertificateFile(this.file);
            ViewUtil.iscNotification(getString(MAINTENANCE_SSLCONFIGURATION_SUCCESSFUL, new Date()),
                    null, Notification.Type.TRAY_NOTIFICATION);
            log.info("=============== Upload certificate succeeded");
            repaintUpload();
        } catch (Exception ex) {
            succeeded = false;
            log.error("=============== Failed to upload certificate", ex);
            ViewUtil.iscNotification("SSL certificate upload failed. " + ex.getMessage() + " Please use a valid certificate file", Notification.Type.ERROR_MESSAGE);
            repaintUpload();
        }

        if(this.uploadNotifier != null){
            this.uploadNotifier.finishedUpload(succeeded);
        }
    }

    protected void processCertificateFile(File file) throws Exception {
        log.info("================ SSL certificate upload completed");
        log.info("================ Adding new entry to truststore...");

        this.x509TrustManager.addEntry(file);
        removeUploadedFile();
    }

    @Override
    public void uploadFailed(FailedEvent event) {
        log.error(new Label(new Date() + ": SSL certificate upload failed.").getValue());

        if (event.getFilename() == null || event.getFilename().isEmpty()) {
            log.warn("No upload certificate file specified");
            ViewUtil.iscNotification(getString(MAINTENANCE_SSLCONFIGURATION_NOFILE),
                    Notification.Type.ERROR_MESSAGE);
            repaintUpload();
        } else if (event.getReason() instanceof UploadInterruptedException) {
            log.warn("SSL certificate upload is cancelled by the user");
        } else {
            log.warn("SSL certificate upload failed");
            ViewUtil.iscNotification(getString(MAINTENANCE_SSLCONFIGURATION_FAILED),
                    Notification.Type.ERROR_MESSAGE);
            repaintUpload();
        }

        removeUploadedFile();
    }

    protected void removeUploadedFile() {
        if (this.file != null && this.file.exists()) {
            try {
                FileUtils.forceDelete(this.file);
            } catch (IOException e) {
                log.error("Deleting ssl certificate file: " + this.file + " failed when upload was cancelled by user.", e);
            }
        }
    }

    public interface UploadNotifier {
        void finishedUpload(boolean uploadStatus);
    }
}
