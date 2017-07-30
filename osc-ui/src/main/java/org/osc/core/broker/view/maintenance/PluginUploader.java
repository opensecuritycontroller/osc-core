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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.view.common.StyleConstants;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.UploadInfoWindow;

import com.vaadin.server.communication.FileUploadHandler.UploadInterruptedException;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.FailedEvent;
import com.vaadin.ui.Upload.FailedListener;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.StartedEvent;
import com.vaadin.ui.Upload.StartedListener;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class PluginUploader extends CustomComponent implements Receiver, FailedListener, SucceededListener {

    private static final Logger log = Logger.getLogger(PluginUploader.class);

    private static int TEMP_FOLDER_COUNTER = 0;
    private final Upload upload;
    private File file;
    private final Panel panel = new Panel();
    private final VerticalLayout verLayout = new VerticalLayout();
    private String uploadPath;

    private final ServerApi server;

    public interface UploadSucceededListener {
        void uploadComplete(String uploadPath);
    }

    private UploadSucceededListener uploadSucceededListener;

    public PluginUploader(ServerApi server) {
        this.server = server;

        this.upload = new Upload();
        this.upload.setButtonCaption("Upload");
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

        this.verLayout.setSpacing(true);
        this.verLayout.addStyleName(StyleConstants.COMPONENT_SPACING);
        this.verLayout.addComponent(this.upload);

        this.panel.setWidth("100%");
        this.panel.setContent(this.verLayout);
        setCompositionRoot(this.panel);
    }

    @Override
    public OutputStream receiveUpload(String filename, String mimeType) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        log.info("Start uploading file: " + filename);
        try {
            if (validateFileExtension(filename)) {
                this.uploadPath = getTemporaryUploadPath();
                File uploadDirectory = new File(this.uploadPath);
                if (!uploadDirectory.exists()) {
                    FileUtils.forceMkdir(uploadDirectory);
                }
                this.file = new File(this.uploadPath + filename);
                return new FileOutputStream(this.file);
            }

        } catch (Exception e) {
            log.error("Error opening file: " + filename, e);
            ViewUtil.iscNotification(VmidcMessages.getString(VmidcMessages_.UPLOAD_COMMON_ERROR) + e.getMessage(),
                    Notification.Type.ERROR_MESSAGE);
        }
        return null;
    }

    /**
     * Gets the upload path for the file. If isTmpFolder is set to true, returns a unique temporary upload file path.
     * If it is set to false it returns the actual image upload folder.
     *
     * @param isTmpFolder
     *            the temporary path the file is uploaded before being moved to the actual image path
     *
     * @return the temporary path when isTmpFolder is set to true
     *         the image path when isTmpFolder is set to false
     */
    private static String getTemporaryUploadPath() {
        String uploadPath = "tmp" + TEMP_FOLDER_COUNTER++ + File.separator;
        return uploadPath;
    }

    @Override
    public void uploadSucceeded(SucceededEvent event) {
        log.info("Upload Successful! Analyzing Uploaded Image.....");

        try {
            // Do the unzip only if there is enough disc space
            if (!this.server.isEnoughSpace()) {
                String message = VmidcMessages.getString(VmidcMessages_.UPLOAD_PLUGIN_NOSPACE);
                throw new VmidcException(message);
            }

            if (!validateFileExtension(this.file.getName())) {
                String message = VmidcMessages.getString(VmidcMessages_.UPLOAD_PLUGIN_FAILED);
                throw new VmidcException(message);
            }

            if (this.uploadSucceededListener != null) {
                this.uploadSucceededListener.uploadComplete(this.uploadPath);
            }

        } catch (Exception e) {

            log.error("Failed to unzip uploaded zip file", e);
            try {
                FileUtils.deleteDirectory(new File(this.uploadPath));
            } catch (IOException ex) {
                log.error("Failed to cleanup the tmp directory", ex);
            }

            this.uploadPath = null;
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }

    @Override
    public void uploadFailed(FailedEvent event) {
        if (event.getFilename() == null || event.getFilename().isEmpty()) {
            String message = VmidcMessages.getString(VmidcMessages_.UPLOAD_PLUGIN_NOFILE);
            ViewUtil.iscNotification(message, Notification.Type.ERROR_MESSAGE);
        } else if (event.getReason() instanceof UploadInterruptedException) {
            log.warn(event.getFilename().toString() + " Plugin upload is cancelled by the user");
        } else {
            String message = VmidcMessages.getString(VmidcMessages_.UPLOAD_PLUGIN_FAILED);
            ViewUtil.iscNotification(message, Notification.Type.WARNING_MESSAGE);
        }
        if (this.uploadPath != null) {
            File uploadDirectory = new File(this.uploadPath);
            if (uploadDirectory.exists()) {
                try {
                    FileUtils.deleteDirectory(uploadDirectory);
                } catch (IOException e) {
                    log.error("Deleting upload directory: " + this.uploadPath
                            + " failed when upload was cancelled by user.", e);
                }
            }
        }
    }

    private boolean validateFileExtension(String fileName) {
        String extension = FilenameUtils.getExtension(fileName);
        return extension.equals("bar");
    }

    public void addSucceededListener(UploadSucceededListener uploadSucceededListener) {
        this.uploadSucceededListener = uploadSucceededListener;
    }

    public String getUploadPath() {
        return this.uploadPath;
    }

}