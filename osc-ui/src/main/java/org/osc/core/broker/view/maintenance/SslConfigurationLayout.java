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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.osc.core.broker.service.api.DeleteSslCertificateServiceApi;
import org.osc.core.broker.service.api.ListSslCertificatesServiceApi;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.DeleteSslEntryRequest;
import org.osc.core.broker.service.response.CertificateBasicInfoModel;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.service.ssl.TruststoreChangedListener;
import org.osc.core.broker.service.ssl.X509TrustManagerApi;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.VmidcWindow;
import org.osc.core.broker.window.WindowUtil;
import org.osc.core.broker.window.button.OkCancelButtonModel;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Item;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

public class SslConfigurationLayout extends FormLayout implements TruststoreChangedListener {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(SslConfigurationLayout.class);
    private final int CERT_MONTHLY_THRESHOLD = 3;

    private Table sslConfigTable;
    private VmidcWindow<OkCancelButtonModel> deleteWindow;

    private DeleteSslCertificateServiceApi deleteSslCertificateService;
    private ListSslCertificatesServiceApi listSslCertificateService;
    private ServiceRegistration<TruststoreChangedListener> registration;

    public SslConfigurationLayout(DeleteSslCertificateServiceApi deleteSslCertificate,
            ListSslCertificatesServiceApi listSslCertificateService,
            X509TrustManagerApi trustManager, BundleContext ctx) {
        super();

        this.deleteSslCertificateService = deleteSslCertificate;
        this.listSslCertificateService = listSslCertificateService;

        SslCertificateUploader certificateUploader = new SslCertificateUploader(trustManager);
        VerticalLayout sslUploadContainer = makeSslUploadContainer(certificateUploader, getString(CERTIFICATE_UPLOAD_TITLE));
        InternalCertReplacementUploader internalCertReplacementUploader = new InternalCertReplacementUploader(trustManager);
        VerticalLayout sslReplaceInternalContainer = makeSslUploadContainer(internalCertReplacementUploader, getString(KEYPAIR_UPLOAD_TITLE));

        VerticalLayout sslListContainer = new VerticalLayout();
        sslListContainer.addComponent(createHeaderForSslList());

        this.sslConfigTable = new Table();
        this.sslConfigTable.setSizeFull();
        this.sslConfigTable.setImmediate(true);
        this.sslConfigTable.addContainerProperty("Alias", String.class, null);
        this.sslConfigTable.addContainerProperty("SHA1 fingerprint", String.class, null);
        this.sslConfigTable.addContainerProperty("Issuer", String.class, null);
        this.sslConfigTable.addContainerProperty("Valid from", Date.class, null);
        this.sslConfigTable.addContainerProperty("Valid until", Date.class, null);
        this.sslConfigTable.addContainerProperty("Algorithm type", String.class, null);
        this.sslConfigTable.addContainerProperty("Delete", Button.class, null);
        this.sslConfigTable.setColumnWidth("Issuer", 200);
        buildSslConfigurationTable();

        Panel sslConfigTablePanel = new Panel();
        sslConfigTablePanel.setContent(this.sslConfigTable);
        sslListContainer.addComponent(sslConfigTablePanel);

        addComponent(sslReplaceInternalContainer);
        addComponent(sslUploadContainer);
        addComponent(sslListContainer);

        this.registration = ctx.registerService(TruststoreChangedListener.class, this, null);
    }

    private VerticalLayout makeSslUploadContainer(SslCertificateUploader certificateUploader, String title) {
        VerticalLayout sslUploadContainer = new VerticalLayout();
        try {
            certificateUploader.setSizeFull();
            certificateUploader.setUploadNotifier(uploadStatus -> {
                if (uploadStatus) {
                    buildSslConfigurationTable();
                }
            });
            sslUploadContainer.addComponent(ViewUtil.createSubHeader(title, null));
            sslUploadContainer.addComponent(certificateUploader);
        } catch (Exception e) {
            log.error("Cannot add upload component. Trust manager factory failed to initialize", e);
            ViewUtil.iscNotification(getString(MAINTENANCE_SSLCONFIGURATION_UPLOAD_INIT_FAILED, new Date()),
                    null, Notification.Type.TRAY_NOTIFICATION);
        }
        return sslUploadContainer;
    }

    private HorizontalLayout createHeaderForSslList() {
        HorizontalLayout header = ViewUtil.createSubHeader("List of available certificates", null);

        Button refresh = new Button();
        refresh.setStyleName(Reindeer.BUTTON_LINK);
        refresh.setDescription("Refresh");
        refresh.setIcon(new ThemeResource("img/Refresh.png"));
        refresh.addClickListener((Button.ClickListener) event -> buildSslConfigurationTable());
        header.addComponent(refresh);
        return header;
    }

    private List<CertificateBasicInfoModel> getPersistenceSslData() {

        List<CertificateBasicInfoModel> basicInfoModels = new ArrayList<>();

        BaseRequest<BaseDto> listRequest = new BaseRequest<>();
        ListResponse<CertificateBasicInfoModel> res;

        try {
            res = this.listSslCertificateService.dispatch(listRequest);
            basicInfoModels = res.getList();
        } catch (Exception e) {
            log.error("Failed to get information from SSL attributes table", e);
            ViewUtil.iscNotification("Failed to get information from SSL attributes table (" + e.getMessage() + ")", Notification.Type.ERROR_MESSAGE);
        }

        return basicInfoModels;
    }

    private void colorizeValidUntilRows() {

        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, this.CERT_MONTHLY_THRESHOLD);

        this.sslConfigTable.setCellStyleGenerator((Table.CellStyleGenerator) (table, itemId, propertyId) -> {
            if (propertyId != null) {
                return null;
            }
            Item item = this.sslConfigTable.getItem(itemId);
            Date validUntil = (Date) item.getItemProperty("Valid until").getValue();
            if (validUntil.before(calendar.getTime())) {
                return "highlight-warning";
            } else {
                return null;
            }
        });
    }

    private void buildSslConfigurationTable() {
        List<CertificateBasicInfoModel> persistenceSslData = getPersistenceSslData();
        this.sslConfigTable.removeAllItems();
        try {
            for (CertificateBasicInfoModel info : persistenceSslData) {
                this.sslConfigTable.addItem(new Object[]{
                        info.getAlias(),
                        info.getSha1Fingerprint(),
                        info.getIssuer(),
                        info.getValidFrom(),
                        info.getValidTo(),
                        info.getAlgorithmType(),
                        createDeleteEntry(info)
                }, info.getAlias().toLowerCase());
            }
        } catch (Exception e) {
            log.error("Cannot build SSL configuration table", e);
            ViewUtil.iscNotification("Fail to get information from SSL attributes table (" + e.getMessage() + ")", Notification.Type.ERROR_MESSAGE);
        }

        // Additional +1 is added for handling vaadin problem with resizing to content table
        this.sslConfigTable.setPageLength(this.sslConfigTable.size() + 1);

        this.sslConfigTable.sort(new Object[]{"Alias"}, new boolean[]{false});

        colorizeValidUntilRows();
    }

    private Button createDeleteEntry(CertificateBasicInfoModel certificateModel) {
        String removeBtnLabel = (certificateModel.isConnected()) ? "Force delete" : "Delete";
        final Button deleteArchiveButton = new Button(removeBtnLabel);
        deleteArchiveButton.setData(certificateModel);
        deleteArchiveButton.addClickListener(this.removeButtonListener);

        if (certificateModel.getAlias().contains(getString(KEYPAIR_INTERNAL_DISPLAY_ALIAS))) {
            deleteArchiveButton.setEnabled(false);
        }

        return deleteArchiveButton;
    }

    private Button.ClickListener removeButtonListener = new Button.ClickListener() {

        private static final long serialVersionUID = 5173505013809394877L;

        @Override
        public void buttonClick(Button.ClickEvent event) {
            final CertificateBasicInfoModel certificateModel = (CertificateBasicInfoModel) event.getButton().getData();
            if (certificateModel.isConnected()) {
                SslConfigurationLayout.this.deleteWindow = WindowUtil.createAlertWindow(
                        getString(MAINTENANCE_SSLCONFIGURATION_FORCE_REMOVE_DIALOG_TITLE),
                        getString(MAINTENANCE_SSLCONFIGURATION_FORCE_REMOVE_DIALOG_CONTENT, certificateModel.getAlias()));
            } else {
                SslConfigurationLayout.this.deleteWindow = WindowUtil.createAlertWindow(
                        getString(MAINTENANCE_SSLCONFIGURATION_REMOVE_DIALOG_TITLE),
                        getString(MAINTENANCE_SSLCONFIGURATION_REMOVE_DIALOG_CONTENT, certificateModel.getAlias()));
            }
            SslConfigurationLayout.this.deleteWindow.getComponentModel().getOkButton().setData(certificateModel.getAlias());
            SslConfigurationLayout.this.deleteWindow.getComponentModel().setOkClickedListener(SslConfigurationLayout.this.acceptRemoveButtonListener);
            ViewUtil.addWindow(SslConfigurationLayout.this.deleteWindow);
        }
    };

    private Button.ClickListener acceptRemoveButtonListener = new Button.ClickListener() {

        private static final long serialVersionUID = 2512910250970666944L;

        @Override
        public void buttonClick(Button.ClickEvent event) {
            final String alias = (String) event.getButton().getData();
            log.info("Removing ssl entry with alias: " + alias);
            boolean succeed;

            DeleteSslEntryRequest deleteRequest = new DeleteSslEntryRequest(alias);

            try {
                SslConfigurationLayout.this.deleteSslCertificateService.dispatch(deleteRequest);
                succeed = true;
            } catch (Exception e) {
                succeed = false;
                log.error("Failed to remove SSL alias from truststore", e);
            }

            SslConfigurationLayout.this.buildSslConfigurationTable();
            SslConfigurationLayout.this.deleteWindow.close();

            String outputMessage = (succeed) ? MAINTENANCE_SSLCONFIGURATION_REMOVED : MAINTENANCE_SSLCONFIGURATION_REMOVE_FAILURE;
            ViewUtil.iscNotification(getString(outputMessage, new Date()), null, Notification.Type.TRAY_NOTIFICATION);
        }
    };

    @Override
    public void truststoreChanged() {
        buildSslConfigurationTable();
    }

    @Override
    public void detach() {
        super.detach();
        try {
            this.registration.unregister();
        } catch (IllegalStateException ise) {
            // This is not a problem, it just means
            // that the UI bundle stopped before this
            // detach call occurred.
        }
    }


}
