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
package org.osc.core.broker.view.vc;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Date;

import org.osc.core.broker.service.response.CertificateBasicInfoModel;
import org.osc.core.broker.service.ssl.CertificateResolverModel;
import org.osc.core.broker.service.ssl.X509TrustManagerApi;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.CRUDBaseApproveWindow;
import org.osc.core.broker.window.button.ApproveCancelButtonModel;
import org.osc.core.ui.LogProvider;
import org.slf4j.Logger;

import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;

@SuppressWarnings("serial")
public class AddSSLCertificateWindow extends CRUDBaseApproveWindow {

    private static final Logger log = LogProvider.getLogger(AddSSLCertificateWindow.class);

    final String CAPTION = "Add SSL certificate";

    private Table sslConfigTable;
    private final ArrayList<CertificateResolverModel> certificateResolverModels;
    private final SSLCertificateWindowInterface sslCertificateWindowInterface;
    private final X509TrustManagerApi trustManager;

    public interface SSLCertificateWindowInterface {
        void submitFormAction(ArrayList<CertificateResolverModel> certificateResolverModels);
        void cancelFormAction();
    }

    public AddSSLCertificateWindow(ArrayList<CertificateResolverModel> certificateResolverModels,
                                   SSLCertificateWindowInterface sslCertificateWindowInterface,
                                   X509TrustManagerApi trustManager) throws Exception {
        super(new ApproveCancelButtonModel());
        this.certificateResolverModels = certificateResolverModels;
        this.sslCertificateWindowInterface = sslCertificateWindowInterface;
        this.trustManager = trustManager;
        createWindow(this.CAPTION);
    }

    @Override
    public void populateForm() throws Exception {
        this.sslConfigTable = new Table();
        this.sslConfigTable.setSizeFull();
        this.sslConfigTable.setImmediate(true);
        this.sslConfigTable.addContainerProperty("Alias", String.class, null);
        this.sslConfigTable.addContainerProperty("SHA1 fingerprint", String.class, null);
        this.sslConfigTable.addContainerProperty("Issuer", String.class, null);
        this.sslConfigTable.addContainerProperty("Valid from", Date.class, null);
        this.sslConfigTable.addContainerProperty("Valid until", Date.class, null);
        this.sslConfigTable.addContainerProperty("Algorithm type", String.class, null);
        this.sslConfigTable.setColumnWidth("Issuer", 200);
        populateSSLConfigTable();
        this.form.addComponent(this.sslConfigTable);
    }

    private void populateSSLConfigTable() {
        this.sslConfigTable.removeAllItems();
        try {
            java.util.List<CertificateBasicInfoModel> certificateInfoList = getCertificateBasicInfoModelList();
            for (CertificateBasicInfoModel info : certificateInfoList) {
                this.sslConfigTable.addItem(new Object[]{
                        info.getAlias(),
                        info.getSha1Fingerprint(),
                        info.getIssuer(),
                        info.getValidFrom(),
                        info.getValidTo(),
                        info.getAlgorithmType(),
                }, info.getSha1Fingerprint());
            }
        } catch (Exception e) {
            log.error("Cannot populate SSL configuration table", e);
        }

        // Additional +1 is added for handling vaadin problem with resizing to content table
        this.sslConfigTable.setPageLength(this.sslConfigTable.size() + 1);
    }

    private ArrayList<CertificateBasicInfoModel> getCertificateBasicInfoModelList() {
        ArrayList<CertificateBasicInfoModel> certificateBasicInfoModels = new ArrayList<>();

        for (CertificateResolverModel basicInfoModel : this.certificateResolverModels) {
            try {
                certificateBasicInfoModels.add(new CertificateBasicInfoModel(
                        basicInfoModel.getAlias(),
                        this.trustManager.getSha1Fingerprint(basicInfoModel.getCertificate()),
                        basicInfoModel.getCertificate().getIssuerDN().getName(),
                        basicInfoModel.getCertificate().getNotBefore(),
                        basicInfoModel.getCertificate().getNotAfter(),
                        basicInfoModel.getCertificate().getSigAlgName(),
                        this.trustManager.certificateToString(basicInfoModel.getCertificate()))
                );
            } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
                log.error("Cannot create certificate basic information model", e);
            }
        }
        return certificateBasicInfoModels;
    }

    @Override
    public boolean validateForm() {
        return true;
    }

    @Override
    public void submitForm() {
        String caption = VmidcMessages.getString(VmidcMessages_.MAINTENANCE_SSLCONFIGURATION_ADDED, new Date());
        for (CertificateResolverModel certObj : this.certificateResolverModels) {
            try {
                this.trustManager.addEntry(certObj.getCertificate(), certObj.getAlias());
            } catch (Exception e) {
                caption = VmidcMessages.getString(VmidcMessages_.MAINTENANCE_SSLCONFIGURATION_FAILED_ADD, new Date());
                log.error("Cannot add new entry in truststore", e);
            }
        }
        ViewUtil.iscNotification(caption, null, Notification.Type.TRAY_NOTIFICATION);

        this.sslCertificateWindowInterface.submitFormAction(this.certificateResolverModels);
        close();
    }

    @Override
    public void cancelForm() {
        this.sslCertificateWindowInterface.cancelFormAction();
        close();
    }
}
