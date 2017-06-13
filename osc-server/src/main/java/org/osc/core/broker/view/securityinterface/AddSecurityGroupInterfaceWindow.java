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

import org.apache.log4j.Logger;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.policy.PolicyDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.securityinterface.AddSecurityGroupInterfaceService;
import org.osc.core.broker.service.securityinterface.SecurityGroupInterfaceDto;
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

    private final ConformService conformService;

    public AddSecurityGroupInterfaceWindow(Long vsId, ConformService conformService) throws Exception {
        super(vsId);
        this.conformService = conformService;
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
                dto.setPolicyId(policyDto.getId());
                dto.setParentId(this.vsId);
                dto.setFailurePolicyType(FailurePolicyType.NA);

                BaseRequest<SecurityGroupInterfaceDto> req = new BaseRequest<SecurityGroupInterfaceDto>();
                req.setDto(dto);

                AddSecurityGroupInterfaceService addService = new AddSecurityGroupInterfaceService(this.conformService);
                addService.dispatch(req);

                close();
            }

        } catch (Exception e) {
            log.error("Fail to add Security Group Interface", e);
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }

}
