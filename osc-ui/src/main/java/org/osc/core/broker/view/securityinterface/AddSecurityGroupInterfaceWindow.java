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
package org.osc.core.broker.view.securityinterface;

import java.util.Arrays;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.AddSecurityGroupInterfaceServiceApi;
import org.osc.core.broker.service.api.ListVirtualSystemPolicyServiceApi;
import org.osc.core.broker.service.dto.PolicyDto;
import org.osc.core.broker.service.dto.SecurityGroupInterfaceDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.sdk.controller.FailurePolicyType;

import com.vaadin.ui.Notification;

public class AddSecurityGroupInterfaceWindow extends BaseSecurityGroupInterfaceWindow {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(AddSecurityGroupInterfaceWindow.class);

    final String CAPTION = "Add Policy Mapping";

    private final AddSecurityGroupInterfaceServiceApi addSecurityGroupInterfaceService;

    public AddSecurityGroupInterfaceWindow(Long vsId, AddSecurityGroupInterfaceServiceApi addSecurityGroupInterfaceService, ListVirtualSystemPolicyServiceApi listVirtualSystemPolicyService) throws Exception {
        super(vsId, listVirtualSystemPolicyService);
        this.addSecurityGroupInterfaceService = addSecurityGroupInterfaceService;
        createWindow(this.CAPTION);
    }

    @Override
    public void populateForm() {
        try {
            this.form.addComponent(getName());
            this.form.addComponent(getPolicy());
            this.form.addComponent(getTag());
        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                SecurityGroupInterfaceDto dto = new SecurityGroupInterfaceDto();
                dto.setName(this.name.getValue().trim());
                dto.setIsUserConfigurable(true);
                dto.setTagValue(Long.parseLong(this.tag.getValue()));

                PolicyDto policyDto = (PolicyDto) this.policy.getValue();
                // Supporting multi-policies from UI is out of scope
                dto.setPolicies(new HashSet<>(Arrays.asList(policyDto)));
                dto.setParentId(this.vsId);
                dto.setFailurePolicyType(FailurePolicyType.NA);

                BaseRequest<SecurityGroupInterfaceDto> req = new BaseRequest<>();
                req.setDto(dto);

                this.addSecurityGroupInterfaceService.dispatch(req);

                close();
            }

        } catch (Exception e) {
            log.error("Fail to add Security Group Interface", e);
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }

}