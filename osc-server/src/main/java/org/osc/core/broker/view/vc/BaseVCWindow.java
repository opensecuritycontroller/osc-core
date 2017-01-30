package org.osc.core.broker.view.vc;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vmware.vim25.InvalidLogin;
import org.apache.log4j.Logger;
import org.jclouds.http.HttpResponseException;
import org.jclouds.rest.AuthorizationException;
import org.osc.core.broker.model.entities.SslCertificateAttr;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.sdncontroller.ControllerType;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.model.virtualization.OpenstackSoftwareVersion;
import org.osc.core.broker.model.virtualization.VirtualizationType;
import org.osc.core.broker.model.virtualization.VmwareSoftwareVersion;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.ErrorTypeException;
import org.osc.core.broker.service.request.ErrorTypeException.ErrorType;
import org.osc.core.broker.service.request.SslCertificatesExtendedException;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.CRUDBaseWindow;
import org.osc.core.broker.window.VmidcWindow;
import org.osc.core.broker.window.WindowUtil;
import org.osc.core.broker.window.button.OkCancelButtonModel;
import org.osc.core.rest.client.crypto.model.CertificateResolverModel;
import org.osc.core.rest.client.exception.RestClientException;
import org.osc.core.util.EncryptionUtil;
import org.osc.sdk.controller.api.SdnControllerApi;

import java.net.ConnectException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class BaseVCWindow extends CRUDBaseWindow<OkCancelButtonModel> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(BaseVCWindow.class);

    public static final String DEFAULT_HTTPS = "false";
    public static final String DEFAULT_RABBITMQ_USER = "guest";
    public static final String DEFAULT_RABBITMQ_USER_PASSWORD = "guest";
    public static final String DEFAULT_RABBITMQ_PORT = "5672";

    protected static final String VCENTER_CAPTION = VmidcMessages.getString(VmidcMessages_.VCENTER);
    protected static final String NSX_CAPTION = VmidcMessages.getString(VmidcMessages_.NSX);
    protected static final String SHOW_ADVANCED_SETTINGS_CAPTION = VmidcMessages
            .getString(VmidcMessages_.SHOW_ADVANCED);
    protected static final String KEYSTONE_CAPTION = VmidcMessages.getString(VmidcMessages_.KEYSTONE);
    protected static final String SDN_CONTROLLER_CAPTION = "SDN Controller";
    protected static final String OPENSTACK_CAPTION = VmidcMessages.getString(VmidcMessages_.KEYSTONE);
    protected Map<String, String> providerAttributes = new HashMap<>();

    protected List<ErrorType> errorTypesToIgnore = new ArrayList<>();
    private HashSet<SslCertificateAttr> sslCertificateAttrs = new HashSet<>();

    // current view referencecurrentVCObject

    protected VirtualizationConnectorView vcView = null;

    protected BeanItem<VirtualizationConnectorDto> currentVCObject = null;

    // form fields
    protected TextField name = null;
    protected ComboBox virtualizationType = null;

    // NSX input fields
    protected TextField controllerIP = null;
    protected TextField controllerUser = null;
    protected PasswordField controllerPW = null;

    // vCenter input fields
    protected TextField providerIP = null;
    protected TextField adminTenantName = null;
    protected TextField providerUser = null;
    protected PasswordField providerPW = null;

    // Open stack Input Fields
    protected ComboBox controllerType = null;

    // All Panels
    protected Panel controllerPanel = null;
    protected Panel providerPanel = null;
    protected Button advancedSettings = null;

    public BaseVCWindow() {
        super();
    }

    @Override
    public boolean validateForm() {
        try {
            this.name.validate();
            this.virtualizationType.validate();
            ControllerType controllerTypeValue = ControllerType.fromText(BaseVCWindow.this.controllerType.getValue().toString());

            if (this.virtualizationType.getValue().toString().equals(VirtualizationType.OPENSTACK.toString()) && !controllerTypeValue.equals(ControllerType.NONE)) {
                SdnControllerApi controller = SdnControllerApiFactory.createNetworkControllerApi(controllerTypeValue);
                if (!controller.isUsingProviderCreds()) {
                    this.controllerIP.validate();
                    ValidateUtil.checkForValidIpAddressFormat(this.controllerIP.getValue());
                    this.controllerUser.validate();
                    this.controllerPW.validate();
                }
            }

            this.providerIP.validate();
            ValidateUtil.checkForValidIpAddressFormat(this.providerIP.getValue());
            if (this.adminTenantName.isVisible()) {
                this.adminTenantName.validate();
            }
            this.providerUser.validate();
            this.providerPW.validate();

            return true;
        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage() + ".", Notification.Type.ERROR_MESSAGE);
        }
        return false;
    }

    protected void buildForm() {
        this.name = new TextField("Name");
        this.name.setImmediate(true);
        this.virtualizationType = new ComboBox("Type");
        this.virtualizationType.setTextInputAllowed(false);
        this.virtualizationType.setNullSelectionAllowed(false);
        this.virtualizationType.addItem(VirtualizationType.VMWARE.toString());
        this.virtualizationType.addItem(VirtualizationType.OPENSTACK.toString());
        this.virtualizationType.select(VirtualizationType.VMWARE.toString());

        // adding not null constraint
        this.name.setRequired(true);
        this.name.setRequiredError("Name cannot be empty");
        this.virtualizationType.setRequired(true);
        this.virtualizationType.setRequiredError("Type cannot be empty");

        this.form.setMargin(true);
        this.form.setSizeUndefined();
        this.form.addComponent(this.name);
        this.name.focus();
        this.form.addComponent(this.virtualizationType);

        this.form.addComponent(controllerPanel());
        this.form.addComponent(providerPanel());

        this.advancedSettings = new Button(SHOW_ADVANCED_SETTINGS_CAPTION);
        this.advancedSettings.setImmediate(true);
        this.advancedSettings.setVisible(false);
        this.advancedSettings.addClickListener(new ClickListener() {

            /**
             *
             */
            private static final long serialVersionUID = 7124022733079327543L;

            @Override
            public void buttonClick(ClickEvent event) {
                advancedSettingsClicked();
            }
        });

        this.form.addComponent(this.advancedSettings);
    }

    // Returns vCenter form
    protected Panel providerPanel() {
        this.providerPanel = new Panel();
        this.providerPanel.setImmediate(true);
        this.providerPanel.setCaption(VCENTER_CAPTION);

        this.providerIP = new TextField("IP");
        this.providerIP.setImmediate(true);
        this.adminTenantName = new TextField("Admin Tenant Name");
        this.adminTenantName.setImmediate(true);
        this.providerUser = new TextField("User Name");
        this.providerUser.setImmediate(true);
        this.providerPW = new PasswordField("Password");
        this.providerPW.setImmediate(true);

        // adding not null constraint
        this.adminTenantName.setRequired(true);
        this.adminTenantName.setRequiredError(this.providerPanel.getCaption() + " Admin Tenant Name cannot be empty");
        this.providerIP.setRequired(true);
        this.providerIP.setRequiredError(this.providerPanel.getCaption() + " IP cannot be empty");
        this.providerUser.setRequired(true);
        this.providerUser.setRequiredError(this.providerPanel.getCaption() + " User Name cannot be empty");
        this.providerPW.setRequired(true);
        this.providerPW.setRequiredError(this.providerPanel.getCaption() + " Password cannot be empty");

        FormLayout providerFormPanel = new FormLayout();
        providerFormPanel.addComponent(this.providerIP);
        providerFormPanel.addComponent(this.adminTenantName);
        providerFormPanel.addComponent(this.providerUser);
        providerFormPanel.addComponent(this.providerPW);
        this.providerPanel.setContent(providerFormPanel);

        return this.providerPanel;
    }

    @SuppressWarnings("serial")
    protected Panel controllerPanel() {
        this.controllerPanel = new Panel();
        this.controllerPanel.setImmediate(true);
        this.controllerPanel.setCaption(NSX_CAPTION);

        this.controllerType = new ComboBox("Type");
        this.controllerType.setTextInputAllowed(false);
        this.controllerType.setNullSelectionAllowed(false);

        for (String ct : ControllerType.values()) {
            this.controllerType.addItem(ct);
        }

        this.controllerType.setVisible(false);

        this.controllerIP = new TextField("IP");
        this.controllerIP.setImmediate(true);
        this.controllerUser = new TextField("User Name");
        this.controllerUser.setImmediate(true);
        this.controllerPW = new PasswordField("Password");
        this.controllerPW.setImmediate(true);

        // adding not null constraint
        this.controllerIP.setRequired(true);
        this.controllerIP.setRequiredError(this.controllerPanel.getCaption() + " IP cannot be empty");
        this.controllerUser.setRequired(true);
        this.controllerUser.setRequiredError(this.controllerPanel.getCaption() + " User Name cannot be empty");
        this.controllerPW.setRequired(true);
        this.controllerPW.setRequiredError(this.controllerPanel.getCaption() + " Password cannot be empty");

        FormLayout nsx = new FormLayout();
        nsx.addComponent(this.controllerType);
        nsx.addComponent(this.controllerIP);
        nsx.addComponent(this.controllerUser);
        nsx.addComponent(this.controllerPW);

        this.controllerPanel.setContent(nsx);

        this.controllerType.addValueChangeListener(new ValueChangeListener() {

            @Override
            public void valueChange(ValueChangeEvent event) {
                updateControllerFields(ControllerType.fromText(BaseVCWindow.this.controllerType.getValue().toString()));
            }
        });
        this.controllerType.select(ControllerType.NONE.toString());

        return this.controllerPanel;
    }

    @SuppressWarnings("serial")
    protected void handleException(final Exception originalException) {
        String caption = VmidcMessages.getString(VmidcMessages_.VC_CONFIRM_CAPTION);
        String contentText = null;
        final Throwable exception;
        if (originalException instanceof ErrorTypeException) {
            ErrorType errorType = ((ErrorTypeException) originalException).getType();
            exception = originalException.getCause();
            if (errorType == ErrorType.PROVIDER_EXCEPTION) {
                if (exception instanceof InvalidLogin) {
                    // VCenter Invalid Credential Exception
                    contentText = VmidcMessages.getString(VmidcMessages_.VC_CONFIRM_CREDS, VCENTER_CAPTION);

                } else if (exception instanceof RemoteException
                        || (RestClientException.isConnectException(exception) && isVMware())) {
                    // VCenter Connect Exception
                    contentText = VmidcMessages.getString(VmidcMessages_.VC_CONFIRM_IP, VCENTER_CAPTION);

                } else if (exception instanceof AuthorizationException) {
                    // keystone Invalid Credential Exception
                    contentText = VmidcMessages.getString(VmidcMessages_.VC_CONFIRM_CREDS, KEYSTONE_CAPTION);

                } else if (exception instanceof HttpResponseException
                        && exception.getCause() instanceof ConnectException
                        || (RestClientException.isConnectException(exception) && isOpenstack())) {
                    // Keystone Connect Exception
                    contentText = VmidcMessages.getString(VmidcMessages_.VC_CONFIRM_IP, KEYSTONE_CAPTION);
                }

            } else if (errorType == ErrorType.CONTROLLER_EXCEPTION) {
                if (RestClientException.isCredentialError(exception)) {
                    if (isVMware()) {
                        // NSX Invalid Credential Exception
                        contentText = VmidcMessages.getString(VmidcMessages_.VC_CONFIRM_CREDS, NSX_CAPTION);
                    }

                    if (isOpenstack()) {
                        // SDN Invalid Credential Exception
                        contentText = VmidcMessages.getString(VmidcMessages_.VC_CONFIRM_CREDS,
                                this.controllerType.getValue().toString());
                    }
                } else if (RestClientException.isConnectException(exception)) {
                    if (isVMware()) {
                        // NSX Connect Exception
                        contentText = VmidcMessages.getString(VmidcMessages_.VC_CONFIRM_IP, NSX_CAPTION);
                    } else if (isOpenstack()) {
                        // SDN Controller Connect Exception
                        contentText = VmidcMessages.getString(VmidcMessages_.VC_CONFIRM_IP,
                                this.controllerType.getValue().toString());
                    }
                } else {
                    if (isVMware()) {
                        // NSX Connect Exception
                        contentText = VmidcMessages.getString(VmidcMessages_.VC_CONFIRM_GENERAL, NSX_CAPTION,
                                exception.getMessage());
                    } else if (isOpenstack()) {
                        // SDN Controller Connect Exception
                        contentText = VmidcMessages.getString(VmidcMessages_.VC_CONFIRM_GENERAL,
                                this.controllerType.getValue().toString(), exception.getMessage());
                    }
                }
            } else if (errorType == ErrorType.RABBITMQ_EXCEPTION) {
                contentText = VmidcMessages.getString(VmidcMessages_.VC_CONFIRM_RABBIT, exception.getMessage());
            } else if (errorType == ErrorType.IP_CHANGED_EXCEPTION) {
                contentText = VmidcMessages.getString(VmidcMessages_.VC_WARNING_IPUPDATE);
            }
        } else if (originalException instanceof RestClientException) {
            RestClientException rce = (RestClientException) originalException;
            handleCatchAllException(new Exception(
                    VmidcMessages.getString(VmidcMessages_.GENERAL_REST_ERROR, rce.getHost(), rce.getMessage()),
                    originalException));
            return;
        } else {
            exception = originalException;
        }

        if (contentText != null) {

            final VmidcWindow<OkCancelButtonModel> alertWindow = WindowUtil.createAlertWindow(caption, contentText);

            alertWindow.getComponentModel().setOkClickedListener(new ClickListener() {

                @Override
                public void buttonClick(ClickEvent event) {
                    try {
                        if (originalException instanceof ErrorTypeException) {
                            ErrorType errorType = ((ErrorTypeException) originalException).getType();
                            BaseVCWindow.this.errorTypesToIgnore.add(errorType);
                        }
                        submitForm();
                    } catch (Exception e) {
                        handleException(e);
                    }
                    alertWindow.close();
                }
            });
            ViewUtil.addWindow(alertWindow);
        } else {
            handleCatchAllException(exception);
        }
    }

    private boolean isVMware() {
        return VirtualizationType.fromText(this.virtualizationType.getValue().toString()) == VirtualizationType.VMWARE;
    }

    private boolean isOpenstack() {
        return VirtualizationType.fromText(this.virtualizationType.getValue().toString()) == VirtualizationType.OPENSTACK;
    }

    void sslAwareHandleException(final Exception originalException){
        if (!(originalException instanceof SslCertificatesExtendedException)) {
            handleException(originalException);
            return;
        }

        SslCertificatesExtendedException unknownException = (SslCertificatesExtendedException) originalException;
        ArrayList<CertificateResolverModel> certificateResolverModels = unknownException.getCertificateResolverModels();
        try {
            ViewUtil.addWindow(new AddSSLCertificateWindow(certificateResolverModels, new AddSSLCertificateWindow.SSLCertificateWindowInterface() {
                @Override
                public void submitFormAction(ArrayList<CertificateResolverModel> certificateResolverModels) {
                    if(certificateResolverModels != null) {
                        sslCertificateAttrs.addAll(certificateResolverModels.stream().map(crm -> new SslCertificateAttr(crm.getAlias(), crm.getSha1())).collect(Collectors.toList()));
                    }
                }
                @Override
                public void cancelFormAction() {
                    handleException(originalException);
                }
            }));
        } catch (Exception e) {
            handleException(originalException);
        }
    }

    /**
     * Create and submits the add connector request.
     *
     */

    protected DryRunRequest<VirtualizationConnectorDto> createRequest() throws Exception {
        DryRunRequest<VirtualizationConnectorDto> request = new DryRunRequest<VirtualizationConnectorDto>();
        request.setDto(new VirtualizationConnectorDto());

        VirtualizationConnectorDto dto = request.getDto();
        dto.setName(this.name.getValue().trim());
        VirtualizationType virtualizationTypeValue = VirtualizationType
                .fromText(((String) this.virtualizationType.getValue()).trim());
        dto.setType(virtualizationTypeValue);

        dto.setControllerIP(this.controllerIP.getValue().trim());
        dto.setControllerUser(this.controllerUser.getValue().trim());
        dto.setControllerPassword(this.controllerPW.getValue().trim());

        dto.setProviderIP(this.providerIP.getValue().trim());
        dto.setProviderUser(this.providerUser.getValue().trim());
        dto.setProviderPassword(this.providerPW.getValue().trim());
        dto.setSslCertificateAttrSet(this.sslCertificateAttrs);

        String tenantName = this.adminTenantName.getValue();
        if (tenantName != null) {
            dto.setAdminTenantName(tenantName.trim());
        }
        if (virtualizationTypeValue.equals(VirtualizationType.OPENSTACK)) {
            dto.setProviderAttributes(this.providerAttributes);
        }

        request.addErrorsToIgnore(this.errorTypesToIgnore);

        // TODO: Future. Get virtualization version this from user.
        if (this.virtualizationType.getValue().equals(VirtualizationType.OPENSTACK.toString())) {
            request.getDto().setSoftwareVersion(OpenstackSoftwareVersion.OS_ICEHOUSE.toString());
            request.getDto()
            .setControllerType(ControllerType.fromText(BaseVCWindow.this.controllerType.getValue().toString()));
        } else {
            request.getDto().setSoftwareVersion(VmwareSoftwareVersion.VMWARE_V5_5.toString());
            request.getDto().setControllerType(ControllerType.fromText(NSX_CAPTION));
        }
        return request;
    }

    protected void advancedSettingsClicked() {
        try {
            ViewUtil.addWindow(new AdvancedSettingsWindow(this));
        } catch (Exception e) {
            ViewUtil.iscNotification(e.toString() + ".", Notification.Type.ERROR_MESSAGE);
        }
    }

    protected void updateForm(String type) {
        this.controllerIP.setValue("");
        this.controllerUser.setValue("");
        this.controllerPW.setValue("");
        this.controllerPanel.setVisible(true);

        if (type.equals(VirtualizationType.OPENSTACK.toString())) {
            this.controllerPanel.setCaption(SDN_CONTROLLER_CAPTION);
            this.providerPanel.setCaption(OPENSTACK_CAPTION);
            this.controllerType.setVisible(true);
            this.controllerType.setValue(ControllerType.NONE.toString());
            updateControllerFields(ControllerType.NONE);
            this.adminTenantName.setVisible(true);
            this.advancedSettings.setVisible(true);
            this.advancedSettings.setCaption(SHOW_ADVANCED_SETTINGS_CAPTION);
            updateProviderFields();

        } else {
            this.controllerPanel.setCaption(NSX_CAPTION);
            this.providerPanel.setCaption(VCENTER_CAPTION);
            updateControllerFields(ControllerType.NONE);
            this.controllerType.setVisible(false);
            this.adminTenantName.setVisible(false);
            this.advancedSettings.setVisible(false);
        }

        this.controllerIP.setRequiredError(this.controllerPanel.getCaption() + " IP cannot be empty");
        this.controllerUser.setRequiredError(this.controllerPanel.getCaption() + " User Name cannot be empty");
        this.controllerPW.setRequiredError(this.controllerPanel.getCaption() + " Password cannot be empty");

        this.providerIP.setRequiredError(this.providerPanel.getCaption() + " IP cannot be empty");
        this.providerUser.setRequiredError(this.providerPanel.getCaption() + " User Name cannot be empty");
        this.providerPW.setRequiredError(this.providerPanel.getCaption() + " Password cannot be empty");
        this.adminTenantName.setRequiredError(this.providerPanel.getCaption() + " Admin Tenant Name cannot be empty");
    }

    private void updateControllerFields(ControllerType type) {
        boolean enableFields = false;

        if (BaseVCWindow.this.virtualizationType.getValue().equals(VirtualizationType.VMWARE.toString())) {
            enableFields = true;
        } else if (!type.equals(ControllerType.NONE)) {
            try {
                SdnControllerApi controller = SdnControllerApiFactory.createNetworkControllerApi(type);
                enableFields = !controller.isUsingProviderCreds();
            } catch (Exception e) {
                log.error("Fail to get controller plugin instance", e);
            }
        }
        BaseVCWindow.this.controllerIP.setEnabled(enableFields);
        BaseVCWindow.this.controllerIP.setRequired(enableFields);
        BaseVCWindow.this.controllerUser.setEnabled(enableFields);
        BaseVCWindow.this.controllerUser.setRequired(enableFields);
        BaseVCWindow.this.controllerPW.setEnabled(enableFields);
        BaseVCWindow.this.controllerPW.setRequired(enableFields);
        if (!enableFields) {
            this.controllerIP.setValue("");
            this.controllerUser.setValue("");
            this.controllerPW.setValue("");
        }
    }

    private void updateProviderFields() {
        if (!BaseVCWindow.this.virtualizationType.getValue().equals(VirtualizationType.VMWARE.toString())) {
            // If user does not click advanced Settings we need to populate attributes with default values..
            this.providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_HTTPS, DEFAULT_HTTPS);
            this.providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER, DEFAULT_RABBITMQ_USER);
            this.providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD,
                    EncryptionUtil.encrypt(DEFAULT_RABBITMQ_USER_PASSWORD));
            this.providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_PORT, DEFAULT_RABBITMQ_PORT);
        }
    }
}
