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

import org.osc.core.broker.service.ssl.X509TrustManagerApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;

public class InternalCertReplacementUploader extends SslCertificateUploader {
    private static final long serialVersionUID = 5863055568539605300L;
    private static final Logger log = LoggerFactory.getLogger(InternalCertReplacementUploader.class);

    public InternalCertReplacementUploader(X509TrustManagerApi x509TrustManager) {
        super(x509TrustManager);
    }

    @Override
    protected void layout(Panel panel) {
        Label warningLabel = new Label(getString(KEYPAIR_UPLOAD_WARN_RESTART));

        this.verLayout.addComponent(warningLabel);

        super.layout(panel);
    }

    @Override
    protected void createUpload() {
        super.createUpload();
        this.upload.setButtonCaption(getString(KEYPAIR_UPLOAD_BUTTON_TXT));
        this.upload.setDescription(getString(KEYPAIR_UPLOAD_DESCR));
    }

    @Override
    protected void processCertificateFile(File file) throws Exception {
        log.info("================ SSL certificate upload completed");
        log.info("================ Replacing internal certificate in truststore...");
        this.x509TrustManager.replaceInternalCertificate(file, true);
        removeUploadedFile();
    }
}
