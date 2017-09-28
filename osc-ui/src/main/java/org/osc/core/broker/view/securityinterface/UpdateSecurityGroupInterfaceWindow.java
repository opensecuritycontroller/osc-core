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

import org.osc.core.broker.service.api.ListVirtualSystemPolicyServiceApi;
import org.osc.core.broker.service.api.UpdateSecurityGroupInterfaceServiceApi;
import org.osc.core.broker.service.dto.PolicyDto;
import org.osc.core.broker.service.dto.SecurityGroupInterfaceDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.ui.LogProvider;
import org.osc.sdk.controller.FailurePolicyType;
import org.slf4j.Logger;

import com.vaadin.ui.Notification;

public class UpdateSecurityGroupInterfaceWindow extends BaseSecurityGroupInterfaceWindow {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Logger log = LogProvider.getLogger(UpdateSecurityGroupInterfaceWindow.class);
    private final SecurityGroupInterfaceDto dto;

    final String CAPTION = "Update Policy Mapping";

    private final UpdateSecurityGroupInterfaceServiceApi updateSecurityGroupInterfaceService;

    public UpdateSecurityGroupInterfaceWindow(SecurityGroupInterfaceDto dto,
            ListVirtualSystemPolicyServiceApi listVirtualSystemPolicyService,
            UpdateSecurityGroupInterfaceServiceApi updateSecurityGroupInterfaceService) throws Exception {
        super(dto.getParentId(), listVirtualSystemPolicyService);
        this.dto = dto;
        this.updateSecurityGroupInterfaceService = updateSecurityGroupInterfaceService;
        createWindow(this.CAPTION);
    }

    @Override
    public void populateForm() {
        try {
            this.form.addComponent(getName());
            this.form.addComponent(getPolicy());
            this.form.addComponent(getTag());

            // filling existing information to our form
            this.name.setValue(this.dto.getName());

			for (Object id : this.policy.getContainerDataSource().getItemIds()) {
				if (this.dto.getPolicies() !=null && !this.dto.getPolicies().isEmpty() && this.dto.getPolicies().iterator().next().getId()
						.equals(this.policy.getContainerDataSource().getContainerProperty(id, "id").getValue())) {
					this.policy.select(id);
				}
			}

            this.tag.setValue(this.dto.getTagValue().toString());

        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                SecurityGroupInterfaceDto newDto = new SecurityGroupInterfaceDto();
                newDto.setId(this.dto.getId());
                newDto.setParentId(this.dto.getParentId());
                newDto.setName(this.name.getValue().trim());
                newDto.setIsUserConfigurable(true);
                newDto.setTagValue(Long.parseLong(this.tag.getValue()));
                PolicyDto policyDto = (PolicyDto) this.policy.getValue();
                newDto.setPolicies(new HashSet<>(Arrays.asList(policyDto)));
                newDto.setFailurePolicyType(FailurePolicyType.NA);

                BaseRequest<SecurityGroupInterfaceDto> req = new BaseRequest<>();
                req.setDto(newDto);

                this.updateSecurityGroupInterfaceService.dispatch(req);

                close();
            }

        } catch (Exception e) {
            log.error("Fail to update Security Group Interface", e);
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }

}
