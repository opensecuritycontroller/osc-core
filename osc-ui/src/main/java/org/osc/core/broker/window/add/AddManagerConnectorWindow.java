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

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.net.ssl.SSLException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.AddApplianceManagerConnectorServiceApi;
import org.osc.core.broker.service.api.plugin.PluginService;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.api.server.ValidationApi;
import org.osc.core.broker.service.dto.SslCertificateAttrDto;
import org.osc.core.broker.service.exceptions.VmidcException;
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
import org.osc.core.common.manager.ManagerType;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;

@SuppressWarnings("serial")
public class AddManagerConnectorWindow extends CRUDBaseWindow<OkCancelButtonModel> {

    private static final long serialVersionUID = 1L;

    final String CAPTION = "Add Manager Connector";

    private static final Logger log = Logger.getLogger(AddManagerConnectorWindow.class);

    // current view reference
    private final ManagerConnectorView mcView;

    // form fields
    private TextField name = null;
    private ComboBox type = null;
    private TextField ip = null;
    private TextField user = null;
    private PasswordField pw = null;
    private PasswordField apiKey = null;
    private ArrayList<CertificateResolverModel> certificateResolverModelsList = null;

    private final AddApplianceManagerConnectorServiceApi addMCService;

    private final PluginService pluginStatusService;

    private final ValidationApi validator;

    private final X509TrustManagerApi trustManager;

    private final ServerApi server;

    public AddManagerConnectorWindow(ManagerConnectorView mcView,
            AddApplianceManagerConnectorServiceApi addMCService,
            PluginService pluginStatusService, ValidationApi validator,
            X509TrustManagerApi trustManager, ServerApi server) throws Exception {
        this.mcView = mcView;
        this.addMCService = addMCService;
        this.pluginStatusService = pluginStatusService;
        this.validator = validator;
        this.trustManager = trustManager;
        this.server = server;
        createWindow(this.CAPTION);
    }

    @Override
    public void populateForm() throws Exception {
        Set<String> managerTypes = this.pluginStatusService.getManagerTypes();
        if (managerTypes.size() == 0) {
            throw new VmidcException("No manager plugins found. Please add Manager plugin to perform action");
        }

        this.name = new TextField("Name");
        this.name.setImmediate(true);
        this.type = new ComboBox("Type");

        this.apiKey = new PasswordField("API Key");
        this.apiKey.setImmediate(true);
        this.apiKey.setVisible(false);
        this.apiKey.setImmediate(true);
        this.apiKey.setRequired(true);
        this.apiKey.setRequiredError("Api Key cannot be empty");

        this.type.setImmediate(true);
        this.type.setTextInputAllowed(false);
        this.type.setNullSelectionAllowed(false);
        for (String mt : this.pluginStatusService.getManagerTypes()) {
            this.type.addItem(mt);
        }

        this.type.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                try {
                    if (AddManagerConnectorWindow.this.pluginStatusService.isKeyAuth(AddManagerConnectorWindow.this.type.getValue()
                            .toString())) {
                        AddManagerConnectorWindow.this.apiKey.setVisible(true);
                        AddManagerConnectorWindow.this.user.setVisible(false);
                        AddManagerConnectorWindow.this.pw.setVisible(false);

                        AddManagerConnectorWindow.this.user.setValue("");
                        AddManagerConnectorWindow.this.pw.setValue("");
                    } else {
                        AddManagerConnectorWindow.this.apiKey.setValue("");
                        AddManagerConnectorWindow.this.apiKey.setVisible(false);

                        AddManagerConnectorWindow.this.user.setVisible(true);
                        AddManagerConnectorWindow.this.pw.setVisible(true);
                    }
                } catch (Exception e) {
                    ViewUtil.showError("Error changing manager type", e);
                }
            }
        });

        this.ip = new TextField("IP");
        this.ip.setImmediate(true);
        this.user = new TextField("User Name");
        this.user.setImmediate(true);
        this.pw = new PasswordField("Password");
        this.pw.setImmediate(true);

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
        this.name.focus();
        this.form.addComponent(this.type);
        this.form.addComponent(this.ip);
        this.form.addComponent(this.user);
        this.form.addComponent(this.pw);
        this.form.addComponent(this.apiKey);

        // select the first entry as default Manager Connector...
        this.type.select(managerTypes.toArray()[0].toString());
    }

    @Override
    public boolean validateForm() {
        try {
            this.name.validate();
            this.type.validate();
            this.ip.validate();
            this.validator.checkValidIpAddress(this.ip.getValue());
            if (this.pluginStatusService.isKeyAuth(this.type.getValue().toString())) {
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
                createAndSubmitRequest(null);
            }
        } catch (Exception e) {
            sslAwareHandleException(e);
        }
    }

    /**
     * Create and submits the add connector request.
     *
     * @param errorTypesToIgnore the errortypes to ignore
     */
    private void createAndSubmitRequest(List<ErrorType> errorTypesToIgnore) throws Exception {
        // creating add request with user entered data
        DryRunRequest<ApplianceManagerConnectorRequest> addRequest = new DryRunRequest<>();
        addRequest.setDto(new ApplianceManagerConnectorRequest());
        addRequest.getDto().setName(this.name.getValue().trim());
        addRequest.getDto().setManagerType(ManagerType.fromText(((String) this.type.getValue()).trim()));
        addRequest.getDto().setIpAddress(this.ip.getValue().trim());
        addRequest.getDto().setUsername(this.user.getValue().trim());
        addRequest.getDto().setPassword(this.pw.getValue().trim());
        addRequest.getDto().setApiKey(this.apiKey.getValue().trim());

        HashSet<SslCertificateAttrDto> sslSet = new HashSet<>();
        if (this.certificateResolverModelsList != null) {
            sslSet.addAll(this.certificateResolverModelsList.stream().map(crm -> new SslCertificateAttrDto(crm.getAlias(), crm.getSha1())).collect(Collectors.toList()));
        }
        addRequest.getDto().setSslCertificateAttrSet(sslSet);

        addRequest.addErrorsToIgnore(errorTypesToIgnore);
        // calling add MC service
        log.info("adding manager connector - " + this.name.getValue().trim());
        BaseJobResponse addResponse = this.addMCService.dispatch(addRequest);
        // adding returned ID to the request DTO object
        addRequest.getDto().setId(addResponse.getId());
        // adding new object to the parent table
        this.mcView.getParentContainer().addItemAt(0, addRequest.getDto().getId(), addRequest.getDto());
        this.mcView.parentTableClicked(addRequest.getDto().getId());
        close();

        ViewUtil.showJobNotification(addResponse.getJobId(), this.server);
    }

    private void sslAwareHandleException(final Exception originalException) {
        if (originalException instanceof SslCertificatesExtendedException) {
            SslCertificatesExtendedException unknownException = (SslCertificatesExtendedException) originalException;
            ArrayList<CertificateResolverModel> certificateResolverModels = unknownException.getCertificateResolverModels();
            try {
                ViewUtil.addWindow(new AddSSLCertificateWindow(certificateResolverModels, new AddSSLCertificateWindow.SSLCertificateWindowInterface() {
                    @Override
                    public void submitFormAction(ArrayList<CertificateResolverModel> certificateResolverModels) {
                        AddManagerConnectorWindow.this.certificateResolverModelsList = certificateResolverModels;
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
            cause = originalException.getCause();
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
                            createAndSubmitRequest(Arrays.asList(ErrorType.MANAGER_CONNECTOR_EXCEPTION));
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
