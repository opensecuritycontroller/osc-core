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
package org.osc.core.broker.view.deploymentspec;

import java.util.HashSet;
import java.util.Set;

import org.osc.core.broker.service.api.AddDeploymentSpecServiceApi;
import org.osc.core.broker.service.api.ListAvailabilityZonesServiceApi;
import org.osc.core.broker.service.api.ListFloatingIpPoolsServiceApi;
import org.osc.core.broker.service.api.ListHostAggregateServiceApi;
import org.osc.core.broker.service.api.ListHostServiceApi;
import org.osc.core.broker.service.api.ListNetworkServiceApi;
import org.osc.core.broker.service.api.ListProjectServiceApi;
import org.osc.core.broker.service.api.ListRegionServiceApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.dto.openstack.AvailabilityZoneDto;
import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.dto.openstack.HostAggregateDto;
import org.osc.core.broker.service.dto.openstack.HostDto;
import org.osc.core.broker.service.dto.openstack.OsNetworkDto;
import org.osc.core.broker.service.dto.openstack.OsProjectDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.view.util.ViewUtil;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.vaadin.ui.Notification;

public class AddDeploymentSpecWindow extends BaseDeploymentSpecWindow {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(AddDeploymentSpecWindow.class);

    final String CAPTION = "Add Deployment Specification";

    private AddDeploymentSpecServiceApi addDeploymentSpecService;

    private ServerApi server;

    public AddDeploymentSpecWindow(Long vsId,
            AddDeploymentSpecServiceApi addDeploymentSpecService, ListAvailabilityZonesServiceApi listAvailabilityZonesService,
            ListFloatingIpPoolsServiceApi listFloatingIpPoolsService, ListHostServiceApi listHostService,
            ListHostAggregateServiceApi listHostAggregateService,
            ListNetworkServiceApi listNetworkService,
            ListRegionServiceApi listRegionService, ListProjectServiceApi listProjectService,
            ServerApi server) throws Exception {
        super(new DeploymentSpecDto().withParentId(vsId), listAvailabilityZonesService,
                listFloatingIpPoolsService, listHostService, listHostAggregateService, listNetworkService,
                listRegionService, listProjectService);
        this.addDeploymentSpecService = addDeploymentSpecService;
        this.server = server;
        createWindow(this.CAPTION);
    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                DeploymentSpecDto dto = new DeploymentSpecDto();
                dto.setName(this.name.getValue().trim());
                dto.setProjectId(((OsProjectDto) this.project.getValue()).getId());
                dto.setProjectName(((OsProjectDto) this.project.getValue()).getName());
                dto.setParentId(this.vsId);
                dto.setCount(this.count.getValue());
                dto.setShared(this.shared.getValue() == null ? false : this.shared.getValue());
                dto.setFloatingIpPoolName((String) this.floatingIpPool.getValue());
                dto.setManagementNetworkId(((OsNetworkDto) this.managementNetwork.getValue()).getId());
                dto.setManagementNetworkName(((OsNetworkDto) this.managementNetwork.getValue()).getName());
                dto.setInspectionNetworkId(((OsNetworkDto) this.inspectionNetwork.getValue()).getId());
                dto.setInspectionNetworkName(((OsNetworkDto) this.inspectionNetwork.getValue()).getName());
                dto.setRegion(this.region.getValue().toString().trim());

                if (this.userOption.getValue() == AVAILABILITY_ZONES) {
                    Set<AvailabilityZoneDto> azSet = new HashSet<AvailabilityZoneDto>();
                    for (Object id : this.optionTable.getItemIds()) {
                        if (this.optionTable.getContainerProperty(id, "Enabled").getValue().equals(true)) {
                            AvailabilityZoneDto azDto = (AvailabilityZoneDto) id;
                            azSet.add(azDto);
                        }
                    }
                    dto.setAvailabilityZones(azSet);
                } else if (this.userOption.getValue() == HOSTS) {
                    Set<HostDto> hostSet = new HashSet<>();
                    for (Object id : this.optionTable.getItemIds()) {
                        if (this.optionTable.getContainerProperty(id, "Enabled").getValue().equals(true)) {
                            HostDto hostDto = (HostDto) id;
                            hostSet.add(hostDto);
                        }
                    }
                    dto.setHosts(hostSet);
                } else if (this.userOption.getValue() == HOST_AGGREGATES) {
                    Set<HostAggregateDto> hostAggrDtoSet = new HashSet<>();
                    for (Object id : this.optionTable.getItemIds()) {
                        if (this.optionTable.getContainerProperty(id, "Enabled").getValue().equals(true)) {
                            HostAggregateDto hostAggrDto = (HostAggregateDto) id;
                            hostAggrDtoSet.add(hostAggrDto);
                        }
                    }
                    dto.setHostAggregates(hostAggrDtoSet);
                }
                BaseRequest<DeploymentSpecDto> req = new BaseRequest<DeploymentSpecDto>();
                req.setDto(dto);

                BaseJobResponse response = this.addDeploymentSpecService.dispatch(req);
                close();

                ViewUtil.showJobNotification(response.getJobId(), this.server);
            }

        } catch (Exception e) {
            log.error("Fail to add DS", e);
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }
}
