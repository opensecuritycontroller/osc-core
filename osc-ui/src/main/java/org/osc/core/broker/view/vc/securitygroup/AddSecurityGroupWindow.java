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
import org.osc.core.broker.service.api.AddSecurityGroupServiceApi;
import org.osc.core.broker.service.api.ListOpenstackMembersServiceApi;
import org.osc.core.broker.service.api.ListRegionByVcIdServiceApi;
import org.osc.core.broker.service.api.ListSecurityGroupMembersBySgServiceApi;
import org.osc.core.broker.service.api.ListProjectByVcIdServiceApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.dto.SecurityGroupDto;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.dto.openstack.OsProjectDto;
import org.osc.core.broker.service.request.AddOrUpdateSecurityGroupRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.view.util.ViewUtil;

import com.vaadin.ui.Notification;

public class AddSecurityGroupWindow extends BaseSecurityGroupWindow {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    final String CAPTION = "Add Security Group";

    private static final Logger log = Logger.getLogger(AddSecurityGroupWindow.class);

    private final AddSecurityGroupServiceApi addSecurityGroupService;

    private final ServerApi server;

    public AddSecurityGroupWindow(VirtualizationConnectorDto vcDto,
            ListOpenstackMembersServiceApi listOpenstackMembersService,
            ListRegionByVcIdServiceApi listRegionByVcIdService,
            ListProjectByVcIdServiceApi listProjectByVcIdServiceApi,
            AddSecurityGroupServiceApi addSecurityGroupService,
            ListSecurityGroupMembersBySgServiceApi listSecurityGroupMembersBySgService,
            ServerApi server) throws Exception {
        super(listOpenstackMembersService, listRegionByVcIdService, listProjectByVcIdServiceApi,
                listSecurityGroupMembersBySgService);
        this.addSecurityGroupService = addSecurityGroupService;
        this.server = server;
        this.currentSecurityGroup = new SecurityGroupDto();
        this.currentSecurityGroup.setParentId(vcDto.getId());
        createWindow(this.CAPTION);
        enableSelection(false);

    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                AddOrUpdateSecurityGroupRequest request = new AddOrUpdateSecurityGroupRequest();

                SecurityGroupDto dto = new SecurityGroupDto();
                dto.setParentId(this.currentSecurityGroup.getParentId());
                dto.setName(this.name.getValue().trim());
                dto.setProjectId(((OsProjectDto) this.project.getValue()).getId());
                dto.setProjectName(((OsProjectDto) this.project.getValue()).getName());
                dto.setProtectAll(this.protectionTypeOption.getValue() == TYPE_ALL);

                request.setDto(dto);
                request.setMembers(getSelectedMembers());

                BaseJobResponse response = this.addSecurityGroupService.dispatch(request);

                close();
                ViewUtil.showJobNotification(response.getJobId(), this.server);
            }

        } catch (Exception e) {
            log.error("Fail to add Security Group", e);
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }

}