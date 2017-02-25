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

import org.apache.log4j.Logger;
import org.jclouds.openstack.keystone.v2_0.domain.Tenant;
import org.jclouds.openstack.neutron.v2.domain.Network;
import org.osc.core.broker.service.AddDeploymentSpecService;
import org.osc.core.broker.service.dto.openstack.AvailabilityZoneDto;
import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.dto.openstack.HostAggregateDto;
import org.osc.core.broker.service.dto.openstack.HostDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.view.util.ViewUtil;

import com.vaadin.ui.Notification;

public class AddDeploymentSpecWindow extends BaseDeploymentSpecWindow {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(AddDeploymentSpecWindow.class);

    final String CAPTION = "Add Deployment Specification";

    public AddDeploymentSpecWindow(Long vsId) throws Exception {
        super(new DeploymentSpecDto().withParentId(vsId));
        createWindow(this.CAPTION);
    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                DeploymentSpecDto dto = new DeploymentSpecDto();
                dto.setName(this.name.getValue().trim());
                dto.setTenantId(((Tenant) this.tenant.getValue()).getId());
                dto.setTenantName(((Tenant) this.tenant.getValue()).getName());
                dto.setParentId(this.vsId);
                dto.setCount(this.count.getValue());
                dto.setShared(this.shared.getValue() == null ? false : this.shared.getValue());
                dto.setFloatingIpPoolName((String) this.floatingIpPool.getValue());
                dto.setManagementNetworkId(((Network) this.managementNetwork.getValue()).getId());
                dto.setManagementNetworkName(((Network) this.managementNetwork.getValue()).getName());
                dto.setInspectionNetworkId(((Network) this.inspectionNetwork.getValue()).getId());
                dto.setInspectionNetworkName(((Network) this.inspectionNetwork.getValue()).getName());
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

                AddDeploymentSpecService addService = new AddDeploymentSpecService();
                BaseJobResponse response = addService.dispatch(req);
                close();

                ViewUtil.showJobNotification(response.getJobId());
            }

        } catch (Exception e) {
            log.error("Fail to add DS", e);
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }
}
