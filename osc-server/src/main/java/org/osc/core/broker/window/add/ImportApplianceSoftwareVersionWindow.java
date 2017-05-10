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
package org.osc.core.broker.window.add;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.ImportApplianceSoftwareVersionServiceApi;
import org.osc.core.broker.service.api.server.ArchiveApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.request.ImportFileRequest;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.maintenance.ApplianceUploader;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.CRUDBaseWindow;
import org.osc.core.broker.window.ProgressIndicatorWindow;
import org.osc.core.broker.window.button.OkCancelButtonModel;

import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;

public class ImportApplianceSoftwareVersionWindow extends CRUDBaseWindow<OkCancelButtonModel> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(ImportApplianceSoftwareVersionWindow.class);

    private ApplianceUploader uploader = null;

    private final ImportApplianceSoftwareVersionServiceApi importApplianceSoftwareVersionService;

    private final ServerApi server;

    private final ArchiveApi archiver;

    public ImportApplianceSoftwareVersionWindow(ImportApplianceSoftwareVersionServiceApi importApplianceSoftwareVersionService,
            ServerApi server, ArchiveApi archiver) throws Exception {
        this.importApplianceSoftwareVersionService = importApplianceSoftwareVersionService;
        this.server = server;
        this.archiver = archiver;
        createWindow("Auto Import Appliance Software Version");
    }

    @Override
    public void populateForm() {
        this.form.setMargin(true);
        this.form.setSizeUndefined();

        this.uploader = new ApplianceUploader();
        this.uploader.setSizeFull();
        this.uploader.getUpload().addSucceededListener(getUploadSucceededListener());

        HorizontalLayout layout = new HorizontalLayout();
        layout.addComponent(this.uploader);
        layout.setCaption(VmidcMessages.getString(VmidcMessages_.UPLOAD_APPLIANCE_CAPTION));

        this.form.addComponent(layout);
    }

    @SuppressWarnings("serial")
    private SucceededListener getUploadSucceededListener() {
        return new SucceededListener() {

            @Override
            public void uploadSucceeded(SucceededEvent event) {
                log.info("Upload Successful! Analyzing Uploaded Image.....");
                final ProgressIndicatorWindow progressIndicatorWindow = new ProgressIndicatorWindow();

                progressIndicatorWindow.setWidth("200px");
                progressIndicatorWindow.setHeight("100px");
                progressIndicatorWindow.setCaption("Processing image ...");

                UI.getCurrent().addWindow(progressIndicatorWindow);
                progressIndicatorWindow.bringToFront();

                Runnable serviceCall = uploadValidationService(progressIndicatorWindow, event);

                ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
                exec.schedule(serviceCall, 1, TimeUnit.MILLISECONDS);
            }
        };
    }

    private Runnable uploadValidationService(final ProgressIndicatorWindow progressIndicatorWindow,
            final SucceededEvent event) {

        return new Runnable() {
            @Override
            public void run() {

                // Make service calls in the UI thread, since the calls will update the UI components
                UI.getCurrent().access(new Runnable() {

                    @Override
                    public void run() {

                        String zipfile = ImportApplianceSoftwareVersionWindow.this.uploader.getUploadPath()
                                + event.getFilename();
                        try {
                            // Do the unzip only if there is enough disc space
                            if (!ImportApplianceSoftwareVersionWindow.this.server.isEnoughSpace()) {
                                throw new VmidcException(VmidcMessages.getString("upload.appliance.nospace"));
                            }

                            if (!FilenameUtils.getExtension(event.getFilename()).equals("zip")) {
                                throw new VmidcException(VmidcMessages
                                        .getString(VmidcMessages_.UPLOAD_APPLIANCE_FAILED));
                            }

                            ImportApplianceSoftwareVersionWindow.this.archiver.unzip(zipfile,
                                    ImportApplianceSoftwareVersionWindow.this.uploader.getUploadPath());
                            // After extraction, we don't need the zip file. Delete the zip file
                            log.info("Delete temporary uploaded zip file after extraction " + zipfile);
                            new File(zipfile).delete();

                            addApplianceImage();

                            ViewUtil.iscNotification(
                                    VmidcMessages.getString(VmidcMessages_.UPLOAD_APPLIANCE_SUCCESSFUL), null,
                                    Notification.Type.TRAY_NOTIFICATION);

                            close();

                        } catch (Exception e) {

                            log.error("Failed to process uploaded zip file", e);
                            try {
                                FileUtils.deleteDirectory(new File(ImportApplianceSoftwareVersionWindow.this.uploader
                                        .getUploadPath()));
                            } catch (IOException ex) {
                                log.error("Failed to cleanup the tmp directory", ex);
                            }

                            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);

                        } finally {
                            log.info("Deleting file " + zipfile);
                            new File(zipfile).delete();
                            progressIndicatorWindow.close();
                        }
                    }
                });
            }

        };
    }

    private void addApplianceImage() throws Exception {
        // creating add request with user entered data
        ImportFileRequest addRequest = new ImportFileRequest(this.uploader.getUploadPath());

        this.importApplianceSoftwareVersionService.dispatch(addRequest);
    }

    @Override
    public boolean validateForm() {
        return true;
    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                this.uploader.getUpload().submitUpload();
            }

        } catch (Exception e) {
            log.info(e.getMessage());
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }

    }
}
