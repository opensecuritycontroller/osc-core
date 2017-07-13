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

import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.UpdateVirtualizationConnectorServiceApi;
import org.osc.core.broker.service.api.plugin.PluginService;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.api.server.ValidationApi;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.VirtualizationConnectorRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.ssl.X509TrustManagerApi;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.common.controller.ControllerType;

public class UpdateVirtualizationConnectorWindow extends BaseVCWindow {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(UpdateVirtualizationConnectorWindow.class);

    final String CAPTION = "Edit Virtualization Connector";

    private final UpdateVirtualizationConnectorServiceApi updateVirtualizationConnectorService;

    private final ServerApi server;

    public UpdateVirtualizationConnectorWindow(VirtualizationConnectorView vcView,
            UpdateVirtualizationConnectorServiceApi updateVirtualizationConnectorService,
            PluginService pluginService, ValidationApi validator,
            X509TrustManagerApi trustManager, ServerApi server,
            EncryptionApi encryption) throws Exception {
        super(pluginService, validator, trustManager, encryption);
        this.server = server;
        this.currentVCObject = vcView.getParentContainer().getItem(vcView.getParentItemId());
        this.updateVirtualizationConnectorService = updateVirtualizationConnectorService;
        createWindow(this.CAPTION);
    }

    @Override
    public void populateForm() {

        buildForm();

        // filling text fields with existing table data

        this.name.setValue(this.currentVCObject.getItemProperty("name").getValue().toString());
        this.virtualizationType.select(this.currentVCObject.getItemProperty("type").getValue().toString());
        this.virtualizationType.setEnabled(false);

        updateForm(this.virtualizationType.getValue().toString());

        VirtualizationConnectorDto vcObject = this.currentVCObject.getBean();
        if (vcObject.isControllerDefined()) {
            this.controllerIP.setValue(vcObject.getControllerIP());
            this.controllerUser.setValue(vcObject.getControllerUser());
            this.controllerPW.setValue(vcObject.getControllerPassword());
            this.controllerType.setValue(vcObject.getControllerType());
        } else {
            this.controllerType.setValue(ControllerType.NONE);
            this.controllerType.setEnabled(true);
        }

        this.providerIP.setValue(vcObject.getProviderIP());
        this.adminDomainId.setValue(vcObject.getAdminDomainId());
        this.adminProjectName.setValue(vcObject.getAdminProjectName());
        this.providerUser.setValue(vcObject.getProviderUser());
        this.providerPW.setValue(vcObject.getProviderPassword());
        this.providerAttributes = vcObject.getProviderAttributes();

    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                // creating add request with user entered data
                DryRunRequest<VirtualizationConnectorRequest> updateRequest = createRequest();
                updateRequest.getDto().setId(this.currentVCObject.getBean().getId());
                log.debug("Updating virtualization connector - " + this.name.getValue().trim());
                // no response needed for update request
                BaseJobResponse response = this.updateVirtualizationConnectorService.dispatch(updateRequest);
                close();
                ViewUtil.showJobNotification(response.getJobId(), this.server);
            }
        } catch (Exception exception) {
            sslAwareHandleException(exception);
        }
    }
}