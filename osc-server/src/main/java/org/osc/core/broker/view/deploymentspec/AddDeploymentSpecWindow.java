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
import org.osc.core.broker.service.api.AddDeploymentSpecServiceApi;
import org.osc.core.broker.service.dto.openstack.AvailabilityZoneDto;
import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.dto.openstack.HostAggregateDto;
import org.osc.core.broker.service.dto.openstack.HostDto;
import org.osc.core.broker.service.dto.openstack.OsNetworkDto;
import org.osc.core.broker.service.dto.openstack.OsTenantDto;
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

private AddDeploymentSpecServiceApi addDeploymentSpecServiceApi;

    public AddDeploymentSpecWindow(Long vsId, AddDeploymentSpecServiceApi addDeploymentSpecServiceApi) throws Exception {
        super(new DeploymentSpecDto().withParentId(vsId));
        this.addDeploymentSpecServiceApi = addDeploymentSpecServiceApi;
        createWindow(this.CAPTION);
    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                DeploymentSpecDto dto = new DeploymentSpecDto();
                dto.setName(this.name.getValue().trim());
                dto.setTenantId(((OsTenantDto) this.tenant.getValue()).getId());
                dto.setTenantName(((OsTenantDto) this.tenant.getValue()).getName());
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

                BaseJobResponse response = this.addDeploymentSpecServiceApi.dispatch(req);
                close();

                ViewUtil.showJobNotification(response.getJobId());
            }

        } catch (Exception e) {
            log.error("Fail to add DS", e);
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }
}
