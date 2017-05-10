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
import org.osc.core.broker.service.api.AddVirtualizationConnectorServiceApi;
import org.osc.core.broker.service.api.plugin.PluginService;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.api.server.ValidationApi;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.VirtualizationConnectorRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.ssl.X509TrustManagerApi;
import org.osc.core.broker.view.util.ViewUtil;

import com.vaadin.data.Property.ValueChangeListener;

@SuppressWarnings("serial")
public class AddVirtualizationConnectorWindow extends BaseVCWindow {

    private final String CAPTION = "Add Virtualization Connector";

    private final AddVirtualizationConnectorServiceApi addVirtualizationConnectorService;

    private final ServerApi server;

    private static final Logger log = Logger.getLogger(AddVirtualizationConnectorWindow.class);

    public AddVirtualizationConnectorWindow(VirtualizationConnectorView vcView, AddVirtualizationConnectorServiceApi addVirtualizationConnectorService,
            PluginService pluginService, ValidationApi validator, X509TrustManagerApi trustManager, ServerApi server,
            EncryptionApi encryption) throws Exception {
        super(pluginService, validator, trustManager, encryption);
        this.vcView = vcView;
        this.addVirtualizationConnectorService = addVirtualizationConnectorService;
        this.server = server;
        createWindow(this.CAPTION);
    }

    @Override
    public void populateForm() {
        buildForm();
        updateForm(this.virtualizationType.getValue().toString());
        this.virtualizationType.addValueChangeListener((ValueChangeListener) event -> updateForm(event.getProperty().getValue().toString()));
    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                // creating add request with user entered data

                DryRunRequest<VirtualizationConnectorRequest> addRequest = createRequest();
                // calling add VC service

                log.info("adding virtualization connector - " + this.name.getValue().trim());
                BaseJobResponse addResponse = this.addVirtualizationConnectorService.dispatch(addRequest);

                // adding returned ID to the request DTO object
                addRequest.getDto().setId(addResponse.getId());

                // adding DTO object in parent Table
                this.vcView.getParentContainer().addItemAt(0, addRequest.getDto().getId(), addRequest.getDto());
                this.vcView.parentTableClicked(addRequest.getDto().getId());
                close();

                ViewUtil.showJobNotification(addResponse.getJobId(), this.server);
            }
        } catch (Exception exception) {
            sslAwareHandleException(exception);
        }
    }

}
