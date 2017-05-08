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
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.RestoreServiceApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.api.server.ValidationApi;
import org.osc.core.broker.service.request.RestoreRequest;
import org.osc.core.broker.view.common.StyleConstants;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.UploadInfoWindow;
import org.osc.core.broker.window.add.PasswordWindow;

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
public class DbRestorer extends CustomComponent implements Receiver, FailedListener, SucceededListener {
    private static final Logger log = Logger.getLogger(DbRestorer.class);
    private final Upload upload;
    private File file;
    private final Panel panel = new Panel();
    private final VerticalLayout verLayout = new VerticalLayout();
    private final RestoreServiceApi restoreService;
    private final ServerApi server;
    private final ValidationApi validator;

    public DbRestorer(RestoreServiceApi restoreService, ServerApi server, ValidationApi validator) {
        this.restoreService = restoreService;
        this.server = server;
        this.validator = validator;
        this.upload = new Upload();
        this.upload.setButtonCaption(VmidcMessages.getString(VmidcMessages_.UPLOAD_RESTORE));
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
        this.panel.setWidth("100%");
        this.panel.setContent(this.verLayout);
        this.verLayout.addComponent(this.upload);
        this.verLayout.addStyleName(StyleConstants.COMPONENT_SPACING);
        setCompositionRoot(this.panel);
    }

    @Override
    public OutputStream receiveUpload(String filename, String mimeType) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }

        // validate uploaded file is a zip file or encrypted backup file
        if (!this.restoreService.isValidBackupFilename(filename)) {
            ViewUtil.iscNotification(
                    VmidcMessages.getString(VmidcMessages_.UPLOAD_RESTORE_INVALID_BACKUP, this.server.getProductName()),
                    Notification.Type.WARNING_MESSAGE);
            return null;
        }

        log.info("Start uploading file: " + filename);

        try {
            this.file = new File(filename);
            return new FileOutputStream(this.file);

        } catch (final java.io.FileNotFoundException e) {

            log.error("Error opening file: " + filename, e);
            ViewUtil.iscNotification(VmidcMessages.getString(VmidcMessages_.UPLOAD_COMMON_ERROR) + e.getMessage(),
                    Notification.Type.ERROR_MESSAGE);
        }
        return null;
    }

    @Override
    public void uploadSucceeded(SucceededEvent event) {
    	log.info("Upload Successful! Restoring Database ......");

    	try {
    		PasswordWindow.SubmitFormListener restoreAction = password -> {
	    		RestoreRequest req = new RestoreRequest();
	            req.setBkpFile(this.file);
	            req.setPassword(password);
	            try {
	            	this.restoreService.dispatch(req);
	                ViewUtil.iscNotification("Upload",
	                        VmidcMessages.getString(VmidcMessages_.UPLOAD_RESTORE_UPLOAD_STARTED, this.server.getProductName()),
	                        Notification.Type.WARNING_MESSAGE);
	            } catch (Exception e) {
	                log.error("Restore Service Failed.", e);
	                ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
	            } finally {
	            	// We can delete the uploaded file now since it has been extracted.
	            	this.file.delete();
	            }
	    	};

    		if(this.restoreService.isValidEncryptedBackupFilename(event.getFilename())) {
    			// ask user for decryption password first
    			PasswordWindow passwordWindow = new PasswordWindow(this.validator);
    	    	passwordWindow.setSubmitFormListener(restoreAction);

    	    	ViewUtil.addWindow(passwordWindow);
    		} else if(this.restoreService.isValidZipBackupFilename(event.getFilename())) {
    			// perform restore without decryption
    			restoreAction.submit(null);
    		}
    	} catch(Exception e) {
    		log.error("Restore Service Failed.", e);
    		// ensure that uploaded file is deleted
    		this.file.delete();

            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
    	}
    }

    @Override
    public void uploadFailed(FailedEvent event) {
        if (event.getFilename() == null || event.getFilename().isEmpty()) {
            ViewUtil.iscNotification(VmidcMessages.getString("upload.restore.nofile"),
                    Notification.Type.ERROR_MESSAGE);
        } else if (event.getReason() instanceof UploadInterruptedException) {
            log.warn("Database backup Upload is cancelled by the user");

        } else {
            ViewUtil.iscNotification(VmidcMessages.getString("upload.restore.invalid.backup"),
                    Notification.Type.WARNING_MESSAGE);
        }
    }

}
