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
package org.osc.core.broker.window;

import org.apache.log4j.Logger;

import com.vaadin.server.ErrorHandler;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.FailedEvent;
import com.vaadin.ui.Upload.FinishedEvent;
import com.vaadin.ui.Upload.StartedEvent;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Window;

public class UploadInfoWindow extends Window implements Upload.FailedListener, Upload.SucceededListener,
        Upload.FinishedListener, Upload.StartedListener, Upload.ProgressListener {

    private static final Logger log = Logger.getLogger(UploadInfoWindow.class);

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final ProgressBar progressBar = new ProgressBar();
    private final Label bytesProcessed = new Label();
    private final Label file = new Label();
    private final Button cancelButton;

    @SuppressWarnings("serial")
    public UploadInfoWindow(final Upload upload) {
        super("Upload Status");
        addStyleName("upload-info");
        center();
        setResizable(false);
        setDraggable(false);
        setImmediate(true);
        setWidth("320px");

        final FormLayout uploadInfoForm = new FormLayout();
        setContent(uploadInfoForm);
        setModal(true);
        setClosable(false);

        uploadInfoForm.setMargin(true);
        this.file.setCaption("File name");
        uploadInfoForm.addComponent(this.file);
        this.progressBar.setCaption("Progress");
        this.progressBar.addStyleName("progressBar");
        this.progressBar.setVisible(false);
        uploadInfoForm.addComponent(this.progressBar);
        this.bytesProcessed.setVisible(false);
        uploadInfoForm.addComponent(this.bytesProcessed);

        upload.addStartedListener(this);
        upload.addProgressListener(this);
        upload.addFailedListener(this);
        upload.addSucceededListener(this);
        upload.addFinishedListener(this);
        upload.setErrorHandler(new ErrorHandler() {
            @Override
            public void error(com.vaadin.server.ErrorEvent event) {
                log.warn("Upload failed: " + event.getThrowable().getMessage());
            }
        });
        this.cancelButton = new Button("Cancel");
        this.cancelButton.addClickListener(new Button.ClickListener() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                upload.interruptUpload();
            }
        });
        this.cancelButton.setVisible(false);
        this.cancelButton.setStyleName("small");
        uploadInfoForm.addComponent(this.cancelButton);

    }

    @Override
    public void uploadStarted(final StartedEvent event) {
        this.progressBar.setValue(0f);
        this.progressBar.setVisible(true);
        this.bytesProcessed.setVisible(true);
        this.file.setValue(event.getFilename());
        this.cancelButton.setVisible(true);
    }

    @Override
    public void uploadFailed(final FailedEvent event) {
        close();
    }

    @Override
    public void uploadSucceeded(final SucceededEvent event) {
        close();
    }

    @Override
    public void uploadFinished(final FinishedEvent event) {
        this.progressBar.setVisible(false);
        this.bytesProcessed.setVisible(false);
        this.cancelButton.setVisible(false);
    }

    @Override
    public void updateProgress(final long readBytes, final long contentLength) {
        if (contentLength == -1) {
            this.progressBar.setIndeterminate(true);
        } else {
            this.progressBar.setIndeterminate(false);
            this.progressBar.setValue(new Float(readBytes / (float) contentLength));
            this.bytesProcessed.setValue("Processed: " + WindowUtil.convertBytesToReadableSize(readBytes) + " of "
                    + WindowUtil.convertBytesToReadableSize(contentLength));
        }
    }

}
