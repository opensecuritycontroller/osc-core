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

import static org.osc.core.common.virtualization.VirtualizationConnectorProperties.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osc.core.broker.service.api.plugin.PluginService;
import org.osc.core.broker.service.api.server.ValidationApi;
import org.osc.core.broker.service.dto.SslCertificateAttrDto;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.exceptions.RestClientException;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.ErrorTypeException;
import org.osc.core.broker.service.request.ErrorTypeException.ErrorType;
import org.osc.core.broker.service.request.VirtualizationConnectorRequest;
import org.osc.core.broker.service.ssl.CertificateResolverModel;
import org.osc.core.broker.service.ssl.SslCertificatesExtendedException;
import org.osc.core.broker.service.ssl.X509TrustManagerApi;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.CRUDBaseWindow;
import org.osc.core.broker.window.VmidcWindow;
import org.osc.core.broker.window.WindowUtil;
import org.osc.core.broker.window.button.OkCancelButtonModel;
import org.osc.core.common.virtualization.VirtualizationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public abstract class BaseVCWindow extends CRUDBaseWindow<OkCancelButtonModel> {

    private static final String OPENSTACK_ICEHOUSE = "Icehouse";
    private static final String KUBERNETES_1_6 = "v1.6";

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(BaseVCWindow.class);

    public static final String DEFAULT_HTTPS = "false";
    public static final String DEFAULT_RABBITMQ_USER = "guest";
    public static final String DEFAULT_RABBITMQ_USER_PASSWORD = "guest";
    public static final String DEFAULT_RABBITMQ_PORT = "5672";

    protected static final String SHOW_ADVANCED_SETTINGS_CAPTION = VmidcMessages
            .getString(VmidcMessages_.SHOW_ADVANCED);
    protected static final String KEYSTONE_CAPTION = VmidcMessages.getString(VmidcMessages_.KEYSTONE);
    protected static final String SDN_CONTROLLER_CAPTION = "SDN Controller";
    protected static final String OPENSTACK_CAPTION = VmidcMessages.getString(VmidcMessages_.KEYSTONE);
    protected Map<String, String> providerAttributes = new HashMap<>();

    protected List<ErrorType> errorTypesToIgnore = new ArrayList<>();
    private HashSet<SslCertificateAttrDto> sslCertificateAttrs = new HashSet<>();

    // current view referencecurrentVCObject

    protected VirtualizationConnectorView vcView = null;

    protected BeanItem<VirtualizationConnectorDto> currentVCObject = null;

    // form fields
    protected TextField name = null;
    protected ComboBox virtualizationType = null;

    // Controller input fields
    protected TextField controllerIP = null;
    protected TextField controllerUser = null;
    protected PasswordField controllerPW = null;

    // Provider input fields
    protected TextField providerIP = null;
    protected TextField adminDomainId = null;
    protected TextField adminProjectName = null;
    protected TextField providerUser = null;
    protected PasswordField providerPW = null;

    // Open stack Input Fields
    protected ComboBox controllerType = null;

    // All Panels
    protected Panel controllerPanel = null;
    protected Panel providerPanel = null;
    protected Button advancedSettings = null;

    private final PluginService pluginService;
    private final ValidationApi validator;

    private final X509TrustManagerApi trustManager;

    public BaseVCWindow(PluginService pluginService, ValidationApi validator,
            X509TrustManagerApi trustManager) {
        super();
        this.pluginService = pluginService;
        this.validator = validator;
        this.trustManager = trustManager;
    }

    @Override
    public boolean validateForm() {
        try {
            this.name.validate();
            this.virtualizationType.validate();

            if (this.virtualizationType.getValue().toString().equals(VirtualizationType.OPENSTACK.toString())) {
                String controllerType = (String) BaseVCWindow.this.controllerType.getValue();
                if (!NO_CONTROLLER_TYPE.equals(controllerType) && !this.pluginService.usesProviderCreds(controllerType)) {
                    this.controllerIP.validate();
                    this.validator.checkValidIpAddress(this.controllerIP.getValue());
                    this.controllerUser.validate();
                    this.controllerPW.validate();
                }
            }

            this.providerIP.validate();
            this.validator.checkValidIpAddress(this.providerIP.getValue());
            if (this.adminDomainId.isVisible()) {
                this.adminDomainId.validate();
            }
            if (this.adminProjectName.isVisible()) {
                this.adminProjectName.validate();
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
        for (VirtualizationType virtualizationType : VirtualizationType.values()) {
            this.virtualizationType.addItem(virtualizationType.toString());
        }

        this.virtualizationType.select(VirtualizationType.OPENSTACK.toString());

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

    // Returns OpenStack form
    protected Panel providerPanel() {
        this.providerPanel = new Panel();
        this.providerPanel.setImmediate(true);
        this.providerPanel.setCaption(OPENSTACK_CAPTION);

        this.providerIP = new TextField("IP");
        this.providerIP.setImmediate(true);
        this.adminDomainId = new TextField("Admin Domain Id");
        this.adminDomainId.setImmediate(true);
        this.adminProjectName = new TextField("Admin Project Name");
        this.adminProjectName.setImmediate(true);
        this.providerUser = new TextField("User Name");
        this.providerUser.setImmediate(true);
        this.providerPW = new PasswordField("Password");
        this.providerPW.setImmediate(true);

        // adding not null constraint
        this.adminDomainId.setRequired(true);
        this.adminDomainId.setRequiredError(this.providerPanel.getCaption() + " Admin Domain Id cannot be empty");
        this.adminProjectName.setRequired(true);
        this.adminProjectName.setRequiredError(this.providerPanel.getCaption() + " Admin Project Name cannot be empty");
        this.providerIP.setRequired(true);
        this.providerIP.setRequiredError(this.providerPanel.getCaption() + " IP cannot be empty");
        this.providerUser.setRequired(true);
        this.providerUser.setRequiredError(this.providerPanel.getCaption() + " User Name cannot be empty");
        this.providerPW.setRequired(true);
        this.providerPW.setRequiredError(this.providerPanel.getCaption() + " Password cannot be empty");

        FormLayout providerFormPanel = new FormLayout();
        providerFormPanel.addComponent(this.providerIP);
        providerFormPanel.addComponent(this.adminDomainId);
        providerFormPanel.addComponent(this.adminProjectName);
        providerFormPanel.addComponent(this.providerUser);
        providerFormPanel.addComponent(this.providerPW);
        this.providerPanel.setContent(providerFormPanel);

        return this.providerPanel;
    }

    protected Panel controllerPanel() {
        this.controllerPanel = new Panel();
        this.controllerPanel.setImmediate(true);
        this.controllerPanel.setCaption(SDN_CONTROLLER_CAPTION);

        this.controllerType = new ComboBox("Type");
        this.controllerType.setTextInputAllowed(false);
        this.controllerType.setNullSelectionAllowed(false);

        this.controllerType.addItem(NO_CONTROLLER_TYPE);
        for (String ct : this.pluginService.getControllerTypes()) {
            this.controllerType.addItem(ct);
        }

        this.controllerType.setVisible(true);

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

        FormLayout sdn = new FormLayout();
        sdn.addComponent(this.controllerType);
        sdn.addComponent(this.controllerIP);
        sdn.addComponent(this.controllerUser);
        sdn.addComponent(this.controllerPW);

        this.controllerPanel.setContent(sdn);

        this.controllerType.addValueChangeListener(event -> updateControllerFields((String) BaseVCWindow.this.controllerType.getValue()));
        this.controllerType.select(NO_CONTROLLER_TYPE);

        return this.controllerPanel;
    }

    protected void handleException(final Exception originalException) {
        String caption = VmidcMessages.getString(VmidcMessages_.VC_CONFIRM_CAPTION);
        String contentText = null;
        final Throwable exception;
        if (originalException instanceof ErrorTypeException) {
            ErrorType errorType = ((ErrorTypeException) originalException).getType();
            exception = originalException.getCause();

            // TODO this exception leaks large amounts of implementation detail out of the API
			if (errorType == ErrorType.PROVIDER_EXCEPTION) {
				if (RestClientException.isConnectException(exception) && isOpenstack()) {
					// Keystone Connect Exception
					contentText = VmidcMessages.getString(VmidcMessages_.VC_CONFIRM_IP, KEYSTONE_CAPTION);
				}
			} else if (errorType == ErrorType.PROVIDER_CONNECT_EXCEPTION && isOpenstack()) {
				// Keystone Connect Exception
				// Handle ConnectionException(Not a standard exception) thrown by OpenStack4j
				contentText = VmidcMessages.getString(VmidcMessages_.VC_CONFIRM_IP, KEYSTONE_CAPTION);
			} else if (errorType == ErrorType.PROVIDER_AUTH_EXCEPTION && isOpenstack()) {
				// Keystone Authentication Exception
				contentText = VmidcMessages.getString(VmidcMessages_.VC_CONFIRM_CREDS, KEYSTONE_CAPTION);
			} else if (errorType == ErrorType.CONTROLLER_EXCEPTION) {
				if (RestClientException.isCredentialError(exception)) {
                    if (isOpenstack()) {
                        // SDN Invalid Credential Exception
                        contentText = VmidcMessages.getString(VmidcMessages_.VC_CONFIRM_CREDS,
                                this.controllerType.getValue().toString());
                    }
                } else if (RestClientException.isConnectException(exception)) {
                    if (isOpenstack()) {
                        // SDN Controller Connect Exception
                        contentText = VmidcMessages.getString(VmidcMessages_.VC_CONFIRM_IP,
                                this.controllerType.getValue().toString());
                    }
                } else {
                    if (isOpenstack()) {
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

            alertWindow.getComponentModel().setOkClickedListener(event -> {
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
            });
            ViewUtil.addWindow(alertWindow);
        } else {
            handleCatchAllException(exception);
        }
    }

    private boolean isOpenstack() {
        return VirtualizationType.fromText(this.virtualizationType.getValue().toString()) == VirtualizationType.OPENSTACK;
    }

    void sslAwareHandleException(final Exception originalException) {
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
                    if (certificateResolverModels != null) {
                        BaseVCWindow.this.sslCertificateAttrs.addAll(
                                certificateResolverModels.stream().map(
                                        crm -> new SslCertificateAttrDto(crm.getAlias(), crm.getSha1())).collect(Collectors.toList()
                                                )
                                );
                    }
                }

                @Override
                public void cancelFormAction() {
                    handleException(originalException);
                }
            }, this.trustManager));
        } catch (Exception e) {
            handleException(originalException);
        }
    }

    /**
     * Create and submits the add connector request.
     */

    protected DryRunRequest<VirtualizationConnectorRequest> createRequest() throws Exception {
        DryRunRequest<VirtualizationConnectorRequest> request = new DryRunRequest<>();
        request.setDto(new VirtualizationConnectorRequest());

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

        String domainId = this.adminDomainId.getValue();
        if (domainId != null) {
            dto.setAdminDomainId(domainId.trim());
        }
        String projectName = this.adminProjectName.getValue();
        if (projectName != null) {
            dto.setAdminProjectName(projectName.trim());
        }
        if (virtualizationTypeValue.equals(VirtualizationType.OPENSTACK)) {
            dto.setProviderAttributes(this.providerAttributes);
        }

        request.addErrorsToIgnore(this.errorTypesToIgnore);

        // TODO: Future. Get virtualization version this from user.
        if (this.virtualizationType.getValue().equals(VirtualizationType.OPENSTACK.toString())) {
            request.getDto().setSoftwareVersion(OPENSTACK_ICEHOUSE);

        } else {
            request.getDto().setSoftwareVersion(KUBERNETES_1_6);
        }

        request.getDto()
        .setControllerType((String) BaseVCWindow.this.controllerType.getValue());
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
            this.controllerType.setValue(NO_CONTROLLER_TYPE);
            updateControllerFields(NO_CONTROLLER_TYPE);
            this.adminDomainId.setVisible(true);
            this.adminProjectName.setVisible(true);
            this.advancedSettings.setVisible(true);
            this.advancedSettings.setCaption(SHOW_ADVANCED_SETTINGS_CAPTION);
            updateProviderFields();

        }

        this.controllerIP.setRequiredError(this.controllerPanel.getCaption() + " IP cannot be empty");
        this.controllerUser.setRequiredError(this.controllerPanel.getCaption() + " User Name cannot be empty");
        this.controllerPW.setRequiredError(this.controllerPanel.getCaption() + " Password cannot be empty");

        this.providerIP.setRequiredError(this.providerPanel.getCaption() + " IP cannot be empty");
        this.providerUser.setRequiredError(this.providerPanel.getCaption() + " User Name cannot be empty");
        this.providerPW.setRequiredError(this.providerPanel.getCaption() + " Password cannot be empty");
        this.adminDomainId.setRequiredError(this.providerPanel.getCaption() + " Admin Domain Id cannot be empty");
        this.adminProjectName.setRequiredError(this.providerPanel.getCaption() + " Admin Project Name cannot be empty");
    }

    private void updateControllerFields(String type) {
        boolean enableFields = false;

        if (!type.equals(NO_CONTROLLER_TYPE)) {
            try {
                enableFields = !this.pluginService.usesProviderCreds(type.toString());
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
        if (BaseVCWindow.this.virtualizationType.getValue().equals(VirtualizationType.OPENSTACK.toString())) {
            // If user does not click advanced Settings we need to populate attributes with default values..
            this.providerAttributes.put(ATTRIBUTE_KEY_HTTPS, DEFAULT_HTTPS);
            this.providerAttributes.put(ATTRIBUTE_KEY_RABBITMQ_USER, DEFAULT_RABBITMQ_USER);
            this.providerAttributes.put(ATTRIBUTE_KEY_RABBITMQ_PORT, DEFAULT_RABBITMQ_PORT);
            this.providerAttributes.put(ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD, DEFAULT_RABBITMQ_USER_PASSWORD);
        }
    }
}
