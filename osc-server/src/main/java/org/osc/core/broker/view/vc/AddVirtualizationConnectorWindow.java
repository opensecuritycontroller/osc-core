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

import com.vaadin.data.Property.ValueChangeListener;
import org.apache.log4j.Logger;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.vc.AddVirtualizationConnectorService;
import org.osc.core.broker.util.StaticRegistry;
import org.osc.core.broker.view.util.ViewUtil;

@SuppressWarnings("serial")
public class AddVirtualizationConnectorWindow extends BaseVCWindow {

    private final String CAPTION = "Add Virtualization Connector";

    private AddVirtualizationConnectorService addVirtualizationConnectorService = StaticRegistry.addVirtualizationConnectorService();

    private static final Logger log = Logger.getLogger(AddVirtualizationConnectorWindow.class);

    public AddVirtualizationConnectorWindow(VirtualizationConnectorView vcView) throws Exception {
        this.vcView = vcView;
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

                DryRunRequest<VirtualizationConnectorDto> addRequest = createRequest();
                // calling add VC service

                log.info("adding virtualization connector - " + this.name.getValue().trim());
                BaseJobResponse addResponse = this.addVirtualizationConnectorService.dispatch(addRequest);

                // adding returned ID to the request DTO object
                addRequest.getDto().setId(addResponse.getId());

                // adding DTO object in parent Table
                this.vcView.getParentContainer().addItemAt(0, addRequest.getDto().getId(), addRequest.getDto());
                this.vcView.parentTableClicked(addRequest.getDto().getId());
                close();

                ViewUtil.showJobNotification(addResponse.getJobId());
            }
        } catch (Exception exception) {
            sslAwareHandleException(exception);
        }
    }

}
