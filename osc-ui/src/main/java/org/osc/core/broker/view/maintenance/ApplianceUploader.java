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
import org.osc.core.broker.view.common.StyleConstants;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.UploadInfoWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class ApplianceUploader extends CustomComponent implements Receiver, FailedListener {
	static final String OVF_UPLOAD_PATH = "data" + File.separator + "ovf" + File.separator;
    private static final Logger log = LoggerFactory.getLogger(ApplianceUploader.class);
    private static int TEMP_FOLDER_COUNTER = 0;
    private final Upload upload;
    private File file;
    private final Panel panel = new Panel();
    private final VerticalLayout verLayout = new VerticalLayout();
    private String uploadPath;

    public ApplianceUploader() {
        this.upload = new Upload();
        this.upload.setButtonCaption(null);
        this.upload.setReceiver(this);
        this.upload.addFailedListener(this);
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

    public Upload getUpload() {
        return this.upload;
    }

    @Override
    public OutputStream receiveUpload(String filename, String mimeType) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        log.info("Start uploading file: " + filename);
        try {
            if (validateZipFile(filename)) {
                this.uploadPath = getUploadPath(true);
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

    public static String getImageFolderPath() {
        return getUploadPath(false);
    }

    public String getUploadPath() {
        return this.uploadPath;
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
    public static String getUploadPath(boolean isTmpFolder) {
        String uploadPath = "";
        uploadPath += OVF_UPLOAD_PATH;
        if (isTmpFolder) {
            uploadPath += "tmp" + TEMP_FOLDER_COUNTER++ + File.separator;
        }
        return uploadPath;
    }

    @Override
    public void uploadFailed(FailedEvent event) {
        if (event.getFilename() == null || event.getFilename().isEmpty()) {
            ViewUtil.iscNotification(VmidcMessages.getString(VmidcMessages_.UPLOAD_APPLIANCE_NOFILE),
                    Notification.Type.ERROR_MESSAGE);
        } else if (event.getReason() instanceof UploadInterruptedException) {
            log.warn("Appliance Image upload is cancelled by the user");
        } else {
            ViewUtil.iscNotification(VmidcMessages.getString(VmidcMessages_.UPLOAD_APPLIANCE_FAILED),
                    Notification.Type.ERROR_MESSAGE);
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

    private boolean validateZipFile(String fileName) {
        return FilenameUtils.getExtension(fileName).equals("zip");
    }
}
