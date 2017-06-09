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
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.server.FileApi;
import org.osc.core.broker.service.exceptions.SecurityException;
import org.osc.core.broker.service.ssl.X509TrustManagerApi;
import org.osc.core.broker.view.common.StyleConstants;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.UploadInfoWindow;

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
import org.osgi.service.component.annotations.Reference;

public class SslCertificateUploader extends CustomComponent implements Receiver, FailedListener, SucceededListener {
    private static final Logger log = Logger.getLogger(SslCertificateUploader.class);

    private static final String UPLOAD_DIR = "/tmp/";
    private static final long serialVersionUID = 1L;

    private Upload upload;
    private File file;
    private final VerticalLayout verLayout = new VerticalLayout();
    private UploadNotifier uploadNotifier = null;
    private X509TrustManagerApi x509TrustManager;

    @Reference
    private FileApi fileApi;

    public SslCertificateUploader(X509TrustManagerApi x509TrustManager) {
        this.x509TrustManager = x509TrustManager;
        // Create vmidc upload folder
        File uploadFolder = new File(SslCertificateUploader.UPLOAD_DIR);
        if (!uploadFolder.exists() && !uploadFolder.mkdir()) {
            log.error("Error creating upload folder");
        }
        createUpload();
        this.verLayout.setSpacing(true);
        Panel panel = new Panel();
        panel.setWidth("100%");
        panel.setContent(this.verLayout);

        this.verLayout.addComponent(this.upload);
        this.verLayout.addStyleName(StyleConstants.COMPONENT_SPACING);
        setCompositionRoot(panel);
    }

    public void setUploadNotifier(UploadNotifier uploadNotifier) {
        this.uploadNotifier = uploadNotifier;
    }

    @Override
    public OutputStream receiveUpload(String filename, String mimeType) {
        if (filename != null && !filename.isEmpty()) {
            log.info("Start uploading certificate: " + filename);

            try {
                this.fileApi.preventPathTraversal(filename,UPLOAD_DIR);
                this.file = new File(UPLOAD_DIR + filename);
                return new FileOutputStream(this.file);
            } catch (final java.io.FileNotFoundException e) {
                log.error("Error opening certificate: " + filename, e);
                ViewUtil.iscNotification(VmidcMessages.getString(VmidcMessages_.UPLOAD_COMMON_ERROR) + filename,
                        Notification.Type.ERROR_MESSAGE);
            } catch (IOException | IllegalStateException e) {
                log.error("Error uploading certifcate: " + filename, e);
                ViewUtil.iscNotification(VmidcMessages.getString(VmidcMessages_.UPLOAD_COMMON_ERROR) + filename,
                        Notification.Type.ERROR_MESSAGE);
            } catch (SecurityException e) {
                log.error("Error uploading certifcate: " + filename, e);
                ViewUtil.iscNotification(VmidcMessages.getString(VmidcMessages_.UPLOAD_COMMON_ERROR) + filename,
                        Notification.Type.ERROR_MESSAGE);
            }
        }
        return null;
    }

    private void createUpload() {
        this.upload = new Upload();
        this.upload.setButtonCaption(VmidcMessages.getString(VmidcMessages_.MAINTENANCE_SSLCONFIGURATION_UPLOAD));
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
            log.info("================ SSL certificate upload completed");
            log.info("================ Adding new entry to truststore...");

            addNewCertificate(this.file);
            ViewUtil.iscNotification(VmidcMessages.getString(VmidcMessages_.MAINTENANCE_SSLCONFIGURATION_SUCCESSFUL, new Date()),
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

    private void addNewCertificate(File file) throws Exception {
        this.x509TrustManager.addEntry(file);
        removeUploadedFile();
    }

    @Override
    public void uploadFailed(FailedEvent event) {
        log.error(new Label(new Date() + ": SSL certificate upload failed."));

        if (event.getFilename() == null || event.getFilename().isEmpty()) {
            log.warn("No upload certificate file specified");
            ViewUtil.iscNotification(VmidcMessages.getString(VmidcMessages_.MAINTENANCE_SSLCONFIGURATION_NOFILE),
                    Notification.Type.ERROR_MESSAGE);
            repaintUpload();
        } else if (event.getReason() instanceof UploadInterruptedException) {
            log.warn("SSL certificate upload is cancelled by the user");
        } else {
            log.warn("SSL certificate upload failed");
            ViewUtil.iscNotification(VmidcMessages.getString(VmidcMessages_.MAINTENANCE_SSLCONFIGURATION_FAILED),
                    Notification.Type.ERROR_MESSAGE);
            repaintUpload();
        }

        removeUploadedFile();
    }

    private void removeUploadedFile() {
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