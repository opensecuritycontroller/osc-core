package org.osc.core.broker.view.vc;

import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import org.apache.log4j.Logger;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.CRUDBaseApproveWindow;
import org.osc.core.broker.window.button.ApproveCancelButtonModel;
import org.osc.core.rest.client.crypto.X509TrustManagerFactory;
import org.osc.core.rest.client.crypto.model.CertificateBasicInfoModel;
import org.osc.core.rest.client.crypto.model.CertificateResolverModel;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Date;

@SuppressWarnings("serial")
public class AddSSLCertificateWindow extends CRUDBaseApproveWindow {

    private static final Logger log = Logger.getLogger(AddSSLCertificateWindow.class);

    final String CAPTION = "Add SSL certificate";

    private Table sslConfigTable;
    private ArrayList<CertificateResolverModel> certificateResolverModels;
    private SSLCertificateWindowInterface sslCertificateWindowInterface;

    public interface SSLCertificateWindowInterface {
        void submitFormAction(ArrayList<CertificateResolverModel> certificateResolverModels);
        void cancelFormAction();
    }

    public AddSSLCertificateWindow(ArrayList<CertificateResolverModel> certificateResolverModels,
                                   SSLCertificateWindowInterface sslCertificateWindowInterface) throws Exception {
        super(new ApproveCancelButtonModel());
        this.certificateResolverModels = certificateResolverModels;
        this.sslCertificateWindowInterface = sslCertificateWindowInterface;
        createWindow(this.CAPTION);
    }

    @Override
    public void populateForm() throws Exception {
        this.sslConfigTable = new Table();
        this.sslConfigTable.setSizeFull();
        this.sslConfigTable.setImmediate(true);
        this.sslConfigTable.addContainerProperty("Alias", String.class, null);
        this.sslConfigTable.addContainerProperty("SHA1 fingerprint", String.class, null);
        this.sslConfigTable.addContainerProperty("Valid from", Date.class, null);
        this.sslConfigTable.addContainerProperty("Valid until", Date.class, null);
        this.sslConfigTable.addContainerProperty("Algorithm type", String.class, null);
        populateSSLConfigTable();
        this.form.addComponent(sslConfigTable);
    }

    private void populateSSLConfigTable() {
        this.sslConfigTable.removeAllItems();
        try {
            java.util.List<CertificateBasicInfoModel> certificateInfoList = getCertificateBasicInfoModelList();
            for (CertificateBasicInfoModel info : certificateInfoList) {
                this.sslConfigTable.addItem(new Object[]{
                        info.getAlias(),
                        info.getSha1Fingerprint(),
                        info.getValidFrom(),
                        info.getValidTo(),
                        info.getAlgorithmType(),
                }, info.getSha1Fingerprint());
            }
        } catch (Exception e) {
            log.error("Cannot populate SSL configuration table", e);
        }
        this.sslConfigTable.setPageLength(this.sslConfigTable.size());
    }

    private ArrayList<CertificateBasicInfoModel> getCertificateBasicInfoModelList() {
        ArrayList<CertificateBasicInfoModel> certificateBasicInfoModels = new ArrayList<>();

        for (CertificateResolverModel basicInfoModel : this.certificateResolverModels) {
            try {
                certificateBasicInfoModels.add(new CertificateBasicInfoModel(
                        basicInfoModel.getAlias(),
                        X509TrustManagerFactory.getSha1Fingerprint(basicInfoModel.getCertificate()),
                        basicInfoModel.getCertificate().getNotBefore(),
                        basicInfoModel.getCertificate().getNotAfter(),
                        basicInfoModel.getCertificate().getSigAlgName(),
                        basicInfoModel.getCertificate())
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
        X509TrustManagerFactory trustManagerFactory = null;

        try {
            trustManagerFactory = X509TrustManagerFactory.getInstance();
        } catch (Exception e) {
            log.error("Cannot initialize trust manager factory", e);
        }

        if (trustManagerFactory != null) {
            for (CertificateResolverModel certObj : this.certificateResolverModels) {
                try {
                    trustManagerFactory.addEntry(certObj.getCertificate(), certObj.getAlias());
                } catch (Exception e) {
                    log.error("Cannot add new entry in truststore", e);
                }
            }
            ViewUtil.iscNotification(VmidcMessages.getString(VmidcMessages_.MAINTENANCE_SSLCONFIGURATION_ADDED,
                    new Date()), null, Notification.Type.TRAY_NOTIFICATION);
        }

        sslCertificateWindowInterface.submitFormAction(this.certificateResolverModels);
        close();
    }

    @Override
    public void cancelForm() {
        sslCertificateWindowInterface.cancelFormAction();
    }
}