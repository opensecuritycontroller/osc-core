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
import org.osc.core.broker.service.api.ListAvailabilityZonesServiceApi;
import org.osc.core.broker.service.api.ListFloatingIpPoolsServiceApi;
import org.osc.core.broker.service.api.ListHostAggregateServiceApi;
import org.osc.core.broker.service.api.ListHostServiceApi;
import org.osc.core.broker.service.api.ListNetworkServiceApi;
import org.osc.core.broker.service.api.ListRegionServiceApi;
import org.osc.core.broker.service.api.ListTenantServiceApi;
import org.osc.core.broker.service.api.UpdateDeploymentSpecServiceApi;
import org.osc.core.broker.service.api.server.ServerApi;
import org.osc.core.broker.service.dto.openstack.AvailabilityZoneDto;
import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.dto.openstack.HostAggregateDto;
import org.osc.core.broker.service.dto.openstack.HostDto;
import org.osc.core.broker.service.dto.openstack.OsNetworkDto;
import org.osc.core.broker.service.dto.openstack.OsTenantDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.ProgressIndicatorWindow;

import com.vaadin.ui.Notification;

public class UpdateDeploymentSpecWindow extends BaseDeploymentSpecWindow {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(UpdateDeploymentSpecWindow.class);
    final String CAPTION = "Edit Deployment Specification";

    private UpdateDeploymentSpecServiceApi updateDeploymentSpecService;

    private ServerApi server;

    public UpdateDeploymentSpecWindow(DeploymentSpecDto dto, UpdateDeploymentSpecServiceApi updateDeploymentSpecService,
            ListAvailabilityZonesServiceApi listAvailabilityZonesService, ListFloatingIpPoolsServiceApi listFloatingIpPoolsService,
            ListHostServiceApi listHostService, ListHostAggregateServiceApi listHostAggregateService, ListNetworkServiceApi listNetworkService, ListRegionServiceApi listRegionService,
            ListTenantServiceApi listTenantService, ServerApi server) throws Exception {
        super(dto, listAvailabilityZonesService, listFloatingIpPoolsService, listHostService, listHostAggregateService,
                listNetworkService, listRegionService, listTenantService);
        this.updateDeploymentSpecService = updateDeploymentSpecService;
        this.server = server;
        createWindow(this.CAPTION);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void makeServiceCalls(ProgressIndicatorWindow progressIndicatorWindow) {
        try {
            super.makeServiceCalls(progressIndicatorWindow);

            // filling existing information to our form
            this.name.setValue(this.deploymentSpecDto.getName());
            for (Object id : this.tenant.getContainerDataSource().getItemIds()) {
                if (this.deploymentSpecDto.getTenantId().equals(
                        this.tenant.getContainerDataSource().getContainerProperty(id, "id").getValue())) {
                    this.tenant.select(id);
                }
            }

            this.tenant.setEnabled(false);

            this.region.setValue(this.deploymentSpecDto.getRegion());
            this.region.setEnabled(false);

            if (this.deploymentSpecDto.getHosts().isEmpty() && this.deploymentSpecDto.getAvailabilityZones().isEmpty()
                    && this.deploymentSpecDto.getHostAggregates().isEmpty()) {
                this.userOption.setValue(NONE);
            } else if (!this.deploymentSpecDto.getHosts().isEmpty()) {
                this.userOption.setValue(HOSTS);
                // select existing Hosts for this DS
                for (HostDto hsDto : this.deploymentSpecDto.getHosts()) {
                    for (Object id : this.optionTable.getItemIds()) {
                        HostDto tableDto = (HostDto) id;
                        if (tableDto != null && tableDto.getOpenstackId().equals(hsDto.getOpenstackId())) {
                            this.optionTable.getContainerProperty(id, "Enabled").setValue(true);
                        }

                    }
                }

            } else if (!this.deploymentSpecDto.getAvailabilityZones().isEmpty()) {
                this.userOption.setValue(AVAILABILITY_ZONES);
                // select existing Availability Zones for this DS

                for (AvailabilityZoneDto azDto : this.deploymentSpecDto.getAvailabilityZones()) {
                    for (Object id : this.optionTable.getItemIds()) {
                        AvailabilityZoneDto tableDto = (AvailabilityZoneDto) id;
                        if (tableDto != null && tableDto.getZone().equals(azDto.getZone())) {
                            this.optionTable.getContainerProperty(id, "Enabled").setValue(true);
                        }
                    }
                }
            } else if (!this.deploymentSpecDto.getHostAggregates().isEmpty()) {
                this.userOption.setValue(HOST_AGGREGATES);
                // select existing Host Aggregates for this DS

                for (HostAggregateDto hostAggrDto : this.deploymentSpecDto.getHostAggregates()) {
                    for (Object id : this.optionTable.getItemIds()) {
                        HostAggregateDto tableDto = (HostAggregateDto) id;
                        if (tableDto != null && tableDto.getOpenstackId().equals(hostAggrDto.getOpenstackId())) {
                            this.optionTable.getContainerProperty(id, "Enabled").setValue(true);
                        }
                    }
                }
            }

            this.userOption.setEnabled(false);

            for (Object id : this.managementNetwork.getContainerDataSource().getItemIds()) {
                if (this.deploymentSpecDto.getManagementNetworkId().equals(
                        this.managementNetwork.getContainerDataSource().getContainerProperty(id, "id").getValue())) {
                    this.managementNetwork.select(id);
                }
            }
            this.managementNetwork.setEnabled(false);

            for (Object id : this.inspectionNetwork.getContainerDataSource().getItemIds()) {
                if (this.deploymentSpecDto.getInspectionNetworkId().equals(
                        this.inspectionNetwork.getContainerDataSource().getContainerProperty(id, "id").getValue())) {
                    this.inspectionNetwork.select(id);
                }
            }
            this.inspectionNetwork.setEnabled(false);

            for (Object id : this.floatingIpPool.getContainerDataSource().getItemIds()) {
                if (id.equals(this.deploymentSpecDto.getFloatingIpPoolName())) {
                    this.floatingIpPool.select(id);
                }
            }
            this.floatingIpPool.setEnabled(false);

            if (this.deploymentSpecDto.getHosts().isEmpty()) {
                this.count.setEnabled(false);
            }
            this.count.setValue(this.deploymentSpecDto.getCount());

            this.shared.setValue(this.deploymentSpecDto.isShared());
            this.shared.setEnabled(false);
        } catch (Exception e) {
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }

    }

    @Override
    public void submitForm() {
        try {
            if (validateForm()) {
                DeploymentSpecDto requestDto = new DeploymentSpecDto();
                requestDto.setId(this.deploymentSpecDto.getId());
                requestDto.setParentId(this.deploymentSpecDto.getParentId());
                requestDto.setName(this.name.getValue().trim());
                requestDto.setTenantId(((OsTenantDto) this.tenant.getValue()).getId());
                requestDto.setTenantName(((OsTenantDto) this.tenant.getValue()).getName());
                requestDto.setCount(this.count.getValue());
                requestDto.setShared(this.shared.getValue() == null ? false : this.shared.getValue());
                requestDto.setFloatingIpPoolName((String) this.floatingIpPool.getValue());
                requestDto.setManagementNetworkId(((OsNetworkDto) this.managementNetwork.getValue()).getId());
                requestDto.setManagementNetworkName(((OsNetworkDto) this.managementNetwork.getValue()).getName());
                requestDto.setInspectionNetworkId(((OsNetworkDto) this.inspectionNetwork.getValue()).getId());
                requestDto.setInspectionNetworkName(((OsNetworkDto) this.inspectionNetwork.getValue()).getName());
                requestDto.setRegion(this.region.getValue().toString().trim());

                if (this.userOption.getValue() == AVAILABILITY_ZONES) {
                    Set<AvailabilityZoneDto> azSet = new HashSet<AvailabilityZoneDto>();
                    for (Object id : this.optionTable.getItemIds()) {
                        if (this.optionTable.getContainerProperty(id, "Enabled").getValue().equals(true)) {
                            AvailabilityZoneDto azDto = (AvailabilityZoneDto) id;
                            azSet.add(azDto);
                        }
                    }
                    requestDto.setAvailabilityZones(azSet);
                } else if (this.userOption.getValue() == HOSTS) {
                    Set<HostDto> hostSet = new HashSet<>();
                    for (Object id : this.optionTable.getItemIds()) {
                        if (this.optionTable.getContainerProperty(id, "Enabled").getValue().equals(true)) {
                            HostDto hostDto = (HostDto) id;
                            hostSet.add(hostDto);
                        }
                    }
                    requestDto.setHosts(hostSet);
                } else if (this.userOption.getValue() == HOST_AGGREGATES) {
                    Set<HostAggregateDto> hostAggrDtoSet = new HashSet<>();
                    for (Object id : this.optionTable.getItemIds()) {
                        if (this.optionTable.getContainerProperty(id, "Enabled").getValue().equals(true)) {
                            HostAggregateDto hostAggrDto = (HostAggregateDto) id;
                            hostAggrDtoSet.add(hostAggrDto);
                        }
                    }
                    requestDto.setHostAggregates(hostAggrDtoSet);
                }

                BaseRequest<DeploymentSpecDto> req = new BaseRequest<DeploymentSpecDto>();
                req.setDto(requestDto);

                BaseJobResponse response = this.updateDeploymentSpecService.dispatch(req);

                close();

                ViewUtil.showJobNotification(response.getJobId(), this.server);
            }

        } catch (Exception e) {
            log.error("Fail to add DS", e);
            ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        }
    }
}