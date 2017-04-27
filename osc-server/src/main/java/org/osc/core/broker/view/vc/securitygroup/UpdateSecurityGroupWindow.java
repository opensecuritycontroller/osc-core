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
package org.osc.core.broker.view.vc.securitygroup;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.api.ListOpenstackMembersServiceApi;
import org.osc.core.broker.service.api.ListRegionByVcIdServiceApi;
import org.osc.core.broker.service.api.ListTenantByVcIdServiceApi;
import org.osc.core.broker.service.api.UpdateSecurityGroupServiceApi;
import org.osc.core.broker.service.dto.SecurityGroupDto;
import org.osc.core.broker.service.dto.openstack.OsTenantDto;
import org.osc.core.broker.service.request.AddOrUpdateSecurityGroupRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.ProgressIndicatorWindow;

import com.vaadin.ui.Notification;

public class UpdateSecurityGroupWindow extends BaseSecurityGroupWindow {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    final String CAPTION = "Update Security Group";

    private static final Logger log = Logger.getLogger(UpdateSecurityGroupWindow.class);

    private UpdateSecurityGroupServiceApi updateSecurityGroupService;

    public UpdateSecurityGroupWindow(SecurityGroupDto dto, ListOpenstackMembersServiceApi listOpenstackMembersService, ListRegionByVcIdServiceApi listRegionByVcIdService,
            ListTenantByVcIdServiceApi listTenantByVcIdServiceApi, UpdateSecurityGroupServiceApi updateSecurityGroupService) throws Exception {
        super(listOpenstackMembersService, listRegionByVcIdService, listTenantByVcIdServiceApi);
        this.currentSecurityGroup = dto;
        this.updateSecurityGroupService = updateSecurityGroupService;
        createWindow(this.CAPTION);
    }

    @Override
    public void makeServiceCalls(ProgressIndicatorWindow progressIndicatorWindow) {
        try {
            super.makeServiceCalls(progressIndicatorWindow);

            this.name.setValue(this.currentSecurityGroup.getName());

            for (Object id : this.tenant.getContainerDataSource().getItemIds()) {
                if (this.currentSecurityGroup.getTenantId().equals(
                        this.tenant.getContainerDataSource().getContainerProperty(id, "id").getValue())) {
                    this.tenant.select(id);
                }
            }
            this.tenant.setEnabled(false);

            // If there is only one region, default to first entry
            if (this.region.size() == 1) {
                this.region.select(this.region.getItemIds().toArray()[0]);
            }

            if (this.currentSecurityGroup.isProtectAll()) {
                enableSelection(false);
            } else {
                this.protectionTypeOption.setValue(TYPE_SELECTION);
                populateToList();
            }

        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {

                AddOrUpdateSecurityGroupRequest request = new AddOrUpdateSecurityGroupRequest();

                SecurityGroupDto newDto = new SecurityGroupDto();
                newDto.setParentId(this.currentSecurityGroup.getParentId());
                newDto.setId(this.currentSecurityGroup.getId());

                newDto.setName(this.name.getValue().trim());
                newDto.setTenantId(((OsTenantDto) this.tenant.getValue()).getId());
                newDto.setTenantName(((OsTenantDto) this.tenant.getValue()).getName());
                newDto.setProtectAll(this.protectionTypeOption.getValue() == TYPE_ALL);

                request.setDto(newDto);
                request.setMembers(getSelectedMembers());

                BaseJobResponse response = this.updateSecurityGroupService.dispatch(request);

                close();
                ViewUtil.showJobNotification(response.getJobId());
            }

        } catch (Exception e) {
            log.error("Fail to update Security Group", e);
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }

}
