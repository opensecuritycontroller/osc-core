package org.osc.core.broker.view.maintenance;

import com.vaadin.data.Item;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import org.apache.log4j.Logger;
import org.osc.core.broker.service.DeleteSslCertificateService;
import org.osc.core.broker.service.ListSslCertificatesService;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.DeleteSslEntryRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.VmidcWindow;
import org.osc.core.broker.window.WindowUtil;
import org.osc.core.broker.window.button.OkCancelButtonModel;
import org.osc.core.rest.client.crypto.X509TrustManagerFactory;
import org.osc.core.rest.client.crypto.model.CertificateBasicInfoModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class SslConfigurationLayout extends FormLayout {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(SslConfigurationLayout.class);
    private final int CERT_MONTHLY_THRESHOLD = 3;
    public static final String INTERNAL_CERTIFICATE_ALIAS = "internal";

    private Table sslConfigTable;
    private VmidcWindow<OkCancelButtonModel> deleteWindow;

    public SslConfigurationLayout() {
        super();

        VerticalLayout sslConfigContainer = new VerticalLayout();

        try {
            SslCertificateUploader certificateUploader = new SslCertificateUploader(X509TrustManagerFactory.getInstance());
            certificateUploader.setSizeFull();
            certificateUploader.setUploadNotifier(uploadStatus -> {
                if (uploadStatus) {
                    buildSslConfigurationTable();
                }
            });
            sslConfigContainer.addComponent(certificateUploader);
        } catch (Exception e) {
            log.error("Cannot add upload component. Trust manager factory failed to initialize", e);
            ViewUtil.iscNotification(VmidcMessages.getString(VmidcMessages_.MAINTENANCE_SSLCONFIGURATION_UPLOAD_INIT_FAILED, new Date()),
                    null, Notification.Type.TRAY_NOTIFICATION);
        }

        sslConfigContainer.addComponent(ViewUtil.createSubHeader("Upload certificate", null));

        this.sslConfigTable = new Table();
        this.sslConfigTable.setSizeFull();
        this.sslConfigTable.setImmediate(true);
        this.sslConfigTable.addContainerProperty("Alias", String.class, null);
        this.sslConfigTable.addContainerProperty("SHA1 fingerprint", String.class, null);
        this.sslConfigTable.addContainerProperty("Valid from", Date.class, null);
        this.sslConfigTable.addContainerProperty("Valid until", Date.class, null);
        this.sslConfigTable.addContainerProperty("Algorithm type", String.class, null);
        this.sslConfigTable.addContainerProperty("Delete", Button.class, null);
        buildSslConfigurationTable();

        Panel sslConfigTablePanel = new Panel();
        sslConfigTablePanel.setContent(this.sslConfigTable);

        addComponent(sslConfigContainer);
        addComponent(sslConfigTablePanel);
    }

    private List<CertificateBasicInfoModel> getPersistenceSslData() {

        List<CertificateBasicInfoModel> basicInfoModels = new ArrayList<>();

        BaseRequest<BaseDto> listRequest = new BaseRequest<>();
        ListResponse<CertificateBasicInfoModel> res;
        ListSslCertificatesService listService = new ListSslCertificatesService();

        try {
            res = listService.dispatch(listRequest);
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
        this.sslConfigTable.setPageLength(this.sslConfigTable.size() + 1);
        this.sslConfigTable.sort(new Object[]{"Alias"}, new boolean[]{false});

        colorizeValidUntilRows();
    }

    @SuppressWarnings("serial")
    private Button createDeleteEntry(CertificateBasicInfoModel certificateModel) {
        String removeBtnLabel = (certificateModel.isConnected()) ? "Force delete" : "Delete";
        final Button deleteArchiveButton = new Button(removeBtnLabel);
        deleteArchiveButton.setData(certificateModel);
        deleteArchiveButton.addClickListener(this.removeButtonListener);

        if (certificateModel.getAlias().contains(SslConfigurationLayout.INTERNAL_CERTIFICATE_ALIAS)) {
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
                        VmidcMessages.getString(VmidcMessages_.MAINTENANCE_SSLCONFIGURATION_FORCE_REMOVE_DIALOG_TITLE),
                        VmidcMessages.getString(VmidcMessages_.MAINTENANCE_SSLCONFIGURATION_FORCE_REMOVE_DIALOG_CONTENT, certificateModel.getAlias()));
            } else {
                SslConfigurationLayout.this.deleteWindow = WindowUtil.createAlertWindow(
                        VmidcMessages.getString(VmidcMessages_.MAINTENANCE_SSLCONFIGURATION_REMOVE_DIALOG_TITLE),
                        VmidcMessages.getString(VmidcMessages_.MAINTENANCE_SSLCONFIGURATION_REMOVE_DIALOG_CONTENT, certificateModel.getAlias()));
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
            DeleteSslCertificateService deleteService = new DeleteSslCertificateService();

            try {
                deleteService.dispatch(deleteRequest);
                succeed = true;
            } catch (Exception e) {
                succeed = false;
                log.error("Failed to remove SSL alias from truststore", e);
            }

            buildSslConfigurationTable();

            SslConfigurationLayout.this.deleteWindow.close();

            String outputMessage = (succeed) ? VmidcMessages_.MAINTENANCE_SSLCONFIGURATION_REMOVED : VmidcMessages_.MAINTENANCE_SSLCONFIGURATION_REMOVE_FAILURE;
            ViewUtil.iscNotification(VmidcMessages.getString(outputMessage, new Date()), null, Notification.Type.TRAY_NOTIFICATION);
        }
    };
}