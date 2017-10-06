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
package org.osc.core.broker.window.update;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.net.ssl.SSLException;

import org.apache.commons.lang.StringEscapeUtils;
import org.osc.core.broker.service.api.UpdateApplianceManagerConnectorServiceApi;
import org.osc.core.broker.service.api.plugin.PluginService;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.api.server.ValidationApi;
import org.osc.core.broker.service.dto.ApplianceManagerConnectorDto;
import org.osc.core.broker.service.dto.SslCertificateAttrDto;
import org.osc.core.broker.service.request.ApplianceManagerConnectorRequest;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.ErrorTypeException;
import org.osc.core.broker.service.request.ErrorTypeException.ErrorType;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.ssl.CertificateResolverModel;
import org.osc.core.broker.service.ssl.SslCertificatesExtendedException;
import org.osc.core.broker.service.ssl.X509TrustManagerApi;
import org.osc.core.broker.view.ManagerConnectorView;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.view.vc.AddSSLCertificateWindow;
import org.osc.core.broker.window.CRUDBaseWindow;
import org.osc.core.broker.window.VmidcWindow;
import org.osc.core.broker.window.WindowUtil;
import org.osc.core.broker.window.button.OkCancelButtonModel;
import org.osc.core.ui.LogProvider;
import org.slf4j.Logger;

import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;

@SuppressWarnings("serial")
public class UpdateManagerConnectorWindow extends CRUDBaseWindow<OkCancelButtonModel> {

    private static final Logger log = LogProvider.getLogger(UpdateManagerConnectorWindow.class);

    protected List<ErrorType> errorTypesToIgnore = new ArrayList<>();

    final String CAPTION = "Edit Manager Connector";

    // current view reference
    private final ManagerConnectorView mcView;

    // form fields
    private TextField name = null;
    private TextField type = null;
    private TextField ip = null;
    private TextField user = null;
    private PasswordField pw = null;
    private PasswordField apiKey = null;

    private final BeanItem<ApplianceManagerConnectorDto> currentMCObject;
    private ArrayList<CertificateResolverModel> certificateResolverModelsList = null;

    private final UpdateApplianceManagerConnectorServiceApi updateMCService;

    private final PluginService pluginService;

    private final ValidationApi validator;

    private final X509TrustManagerApi trustManager;

    private final ServerApi server;

    public UpdateManagerConnectorWindow(ManagerConnectorView mcView,
            UpdateApplianceManagerConnectorServiceApi updateMCService,
            PluginService pluginService, ValidationApi validator,
            X509TrustManagerApi trustManager, ServerApi server) throws Exception {
        this.mcView = mcView;
        this.updateMCService = updateMCService;
        this.pluginService = pluginService;
        this.validator = validator;
        this.trustManager = trustManager;
        this.server = server;
        this.currentMCObject = mcView.getParentContainer().getItem(mcView.getParentItemId());
        createWindow(this.CAPTION);
    }

    @Override
    public void populateForm() throws Exception {

        this.name = new TextField("Name");
        this.name.setImmediate(true);
        this.type = new TextField("Type");
        this.type.setImmediate(true);
        this.type.setEnabled(false);
        this.ip = new TextField("IP");
        this.ip.setImmediate(true);
        this.user = new TextField("UserName");
        this.user.setImmediate(true);
        this.pw = new PasswordField("Password");
        this.pw.setImmediate(true);

        this.apiKey = new PasswordField("API Key");
        this.apiKey.setVisible(false);
        this.apiKey.setImmediate(true);
        this.apiKey.setRequired(true);
        this.apiKey.setRequiredError("Api Key cannot be empty");

        // filling fields with existing information
        this.name.setValue(this.currentMCObject.getItemProperty("name").getValue().toString());
        this.type.setValue(this.currentMCObject.getItemProperty("managerType").getValue().toString());
        this.ip.setValue(this.currentMCObject.getItemProperty("ipAddress").getValue().toString());
        if (this.pluginService.isKeyAuth(this.currentMCObject.getItemProperty("managerType")
                .getValue().toString())) {
            this.apiKey.setVisible(true);
            this.apiKey.setValue(this.currentMCObject.getItemProperty("apiKey").getValue().toString());

            this.user.setVisible(false);
            this.user.setValue("");

            this.pw.setVisible(false);
            this.pw.setValue("");

        } else {
            this.apiKey.setVisible(false);

            this.user.setVisible(true);
            this.user.setValue(this.currentMCObject.getItemProperty("username").getValue().toString());

            this.pw.setVisible(true);
            this.pw.setValue(this.currentMCObject.getItemProperty("password").getValue().toString());
        }

        // adding not null constraint
        this.name.setRequired(true);
        this.name.setRequiredError("Name cannot be empty");
        this.type.setRequired(true);
        this.type.setRequiredError("Type cannot be empty");
        this.ip.setRequired(true);
        this.ip.setRequiredError("IP cannot be empty");
        this.user.setRequired(true);
        this.user.setRequiredError("User Name cannot be empty");
        this.pw.setRequired(true);
        this.pw.setRequiredError("Password cannot be empty");

        this.form.setMargin(true);
        this.form.setSizeUndefined();
        this.form.addComponent(this.name);
        this.form.addComponent(this.type);
        this.form.addComponent(this.ip);
        this.ip.focus();
        this.form.addComponent(this.user);
        this.form.addComponent(this.pw);
        this.form.addComponent(this.apiKey);

    }

    @Override
    public boolean validateForm() {
        try {
            this.name.validate();
            this.type.validate();
            this.ip.validate();
            this.validator.checkValidIpAddress(this.ip.getValue());
            if (this.pluginService.isKeyAuth(this.type.getValue().toString())) {
                this.apiKey.validate();
            } else {
                this.user.validate();
                this.pw.validate();
            }
            return true;
        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage() + ".", Notification.Type.ERROR_MESSAGE);
        }
        return false;
    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                createAndSubmitRequest();
            }
        } catch (Exception exception) {
            sslAwareHandleException(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private void createAndSubmitRequest() throws Exception {
        // creating update request with user modified data
        DryRunRequest<ApplianceManagerConnectorRequest> updateRequest = new DryRunRequest<>();
        updateRequest.setDto(new ApplianceManagerConnectorRequest());
        updateRequest.getDto().setId(this.currentMCObject.getBean().getId());
        updateRequest.getDto().setName(this.name.getValue().trim());
        updateRequest.getDto().setManagerType(this.type.getValue().trim());
        updateRequest.getDto().setIpAddress(this.ip.getValue().trim());
        updateRequest.getDto().setUsername(this.user.getValue().trim());
        updateRequest.getDto().setPassword(this.pw.getValue().trim());
        updateRequest.getDto().setApiKey(this.apiKey.getValue().trim());

        HashSet<SslCertificateAttrDto> sslSet = new HashSet<>();
        if (this.certificateResolverModelsList != null) {
            sslSet.addAll(this.certificateResolverModelsList.stream().map(crm -> new SslCertificateAttrDto(crm.getAlias(), crm.getSha1())).collect(Collectors.toList()));
            updateRequest.getDto().setSslCertificateAttrSet(sslSet);
        }

        updateRequest.addErrorsToIgnore(this.errorTypesToIgnore);

        log.debug("Updating manager connector - " + this.name.getValue().trim());
        // no response needed for update request
        BaseJobResponse response = this.updateMCService.dispatch(updateRequest);

        // updating bean in the table container
        this.mcView.getParentContainer().getContainerProperty(updateRequest.getDto().getId(), "name")
        .setValue(this.name.getValue().trim());
        this.mcView.getParentContainer().getContainerProperty(updateRequest.getDto().getId(), "managerType")
        .setValue(this.type.getValue().trim());
        this.mcView.getParentContainer().getContainerProperty(updateRequest.getDto().getId(), "ipAddress")
        .setValue(this.ip.getValue().trim());
        this.mcView.getParentContainer().getContainerProperty(updateRequest.getDto().getId(), "username")
        .setValue(this.user.getValue().trim());
        this.mcView.getParentContainer().getContainerProperty(updateRequest.getDto().getId(), "password")
        .setValue(this.pw.getValue().trim());
        this.mcView.getParentContainer().getContainerProperty(updateRequest.getDto().getId(), "apiKey")
        .setValue(this.apiKey.getValue().trim());

        close();

        ViewUtil.showJobNotification(response.getJobId(), this.server);
    }

    protected void sslAwareHandleException(final Exception originalException){
        if (originalException instanceof SslCertificatesExtendedException) {
            SslCertificatesExtendedException sslCertificateException = (SslCertificatesExtendedException) originalException;
            ArrayList<CertificateResolverModel> certificateResolverModels = sslCertificateException.getCertificateResolverModels();
            try {
                ViewUtil.addWindow(new AddSSLCertificateWindow(certificateResolverModels, new AddSSLCertificateWindow.SSLCertificateWindowInterface() {
                    @Override
                    public void submitFormAction(ArrayList<CertificateResolverModel> certificateResolverModels) {
                        UpdateManagerConnectorWindow.this.certificateResolverModelsList = certificateResolverModels;
                    }

                    @Override
                    public void cancelFormAction() {
                        handleException(originalException);
                    }
                }, this.trustManager));
            } catch (Exception e) {
                handleException(originalException);
            }
        } else {
            handleException(originalException);
        }
    }

    private void handleException(final Exception originalException) {
        String caption = VmidcMessages.getString(VmidcMessages_.MC_CONFIRM_CAPTION);
        String contentText = null;
        final Throwable cause;
        if (originalException instanceof ErrorTypeException) {
            ErrorType errorType = ((ErrorTypeException) originalException).getType();
            cause = originalException.getCause();
            if (errorType == ErrorType.MANAGER_CONNECTOR_EXCEPTION) {
                if (cause instanceof SocketException
                        || cause instanceof SSLException
                        ||  cause instanceof SocketTimeoutException) {
                    contentText = VmidcMessages.getString(VmidcMessages_.MC_CONFIRM_IP,
                            StringEscapeUtils.escapeHtml(this.name.getValue()));
                } else {
                    handleCatchAllException(new Exception(VmidcMessages.getString(
                            VmidcMessages_.GENERAL_REST_ERROR, this.ip.getValue(), cause.getMessage()),
                            cause));
                    return;
                }
            } else if (errorType == ErrorType.IP_CHANGED_EXCEPTION) {
                contentText = VmidcMessages.getString(VmidcMessages_.MC_WARNING_IPUPDATE);
            }
        } else {
            cause = originalException;
        }
        if (contentText != null) {

            final VmidcWindow<OkCancelButtonModel> alertWindow = WindowUtil.createAlertWindow(caption, contentText);

            alertWindow.getComponentModel().setOkClickedListener(new ClickListener() {

                @Override
                public void buttonClick(ClickEvent event) {
                    if (validateForm()) {
                        try {
                            if (originalException instanceof ErrorTypeException) {
                                ErrorType errorType = ((ErrorTypeException) originalException).getType();
                                UpdateManagerConnectorWindow.this.errorTypesToIgnore.add(errorType);
                            }
                            createAndSubmitRequest();

                        } catch (Exception e) {
                            handleException(e);
                        }
                    }
                    alertWindow.close();
                }
            });
            ViewUtil.addWindow(alertWindow);
        } else {
            handleCatchAllException(cause);
        }
    }
}
