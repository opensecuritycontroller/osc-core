/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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
package org.osc.core.broker.service;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.IscEntity;
import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.events.Alarm;
import org.osc.core.broker.model.entities.events.Alert;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.model.entities.job.TaskRecord;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.AvailabilityZone;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.alarm.AlarmDto;
import org.osc.core.broker.service.alert.AlertDto;
import org.osc.core.broker.service.dto.ApplianceDto;
import org.osc.core.broker.service.dto.ApplianceManagerConnectorDto;
import org.osc.core.broker.service.dto.ApplianceSoftwareVersionDto;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.dto.DistributedApplianceInstanceDto;
import org.osc.core.broker.service.dto.JobRecordDto;
import org.osc.core.broker.service.dto.TaskRecordDto;
import org.osc.core.broker.service.dto.UserDto;
import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.dto.openstack.AvailabilityZoneDto;
import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.AlarmEntityMgr;
import org.osc.core.broker.service.persistence.AlertEntityMgr;
import org.osc.core.broker.service.persistence.ApplianceEntityMgr;
import org.osc.core.broker.service.persistence.ApplianceManagerConnectorEntityMgr;
import org.osc.core.broker.service.persistence.ApplianceSoftwareVersionEntityMgr;
import org.osc.core.broker.service.persistence.AvailabilityZoneEntityMgr;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.persistence.DistributedApplianceEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.JobEntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.persistence.SecurityGroupInterfaceEntityMgr;
import org.osc.core.broker.service.persistence.TaskEntityMgr;
import org.osc.core.broker.service.persistence.UserEntityMgr;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.GetDtoFromEntityRequest;
import org.osc.core.broker.service.response.BaseDtoResponse;
import org.osc.core.broker.service.securitygroup.SecurityGroupDto;
import org.osc.core.broker.service.securityinterface.SecurityGroupInterfaceDto;

public class GetDtoFromEntityService<R extends BaseDto> extends
ServiceDispatcher<GetDtoFromEntityRequest, BaseDtoResponse<R>> {

    @SuppressWarnings("unchecked")
    @Override
    public BaseDtoResponse<R> exec(GetDtoFromEntityRequest request, Session session) throws Exception {

        String entityName = request.getEntityName();
        long entityId = request.getEntityId();
        BaseDtoResponse<R> res = new BaseDtoResponse<R>();
        if (entityName.equals("JobRecord")) {
            JobRecord entity = getEntity(entityId, entityName, JobRecord.class, session);
            JobRecordDto dto = new JobRecordDto();
            JobEntityManager.fromEntity(entity, dto);
            res.setDto((R) dto);
        } else if (entityName.equals("TaskRecord")) {
            TaskRecord entity = getEntity(entityId, entityName, TaskRecord.class, session);
            TaskRecordDto dto = new TaskRecordDto();
            TaskEntityMgr.fromEntity(entity, dto);
            res.setDto((R) dto);
        } else if (entityName.equals("VirtualizationConnector")) {
            VirtualizationConnector entity = getEntity(entityId, entityName, VirtualizationConnector.class, session);
            VirtualizationConnectorDto dto = new VirtualizationConnectorDto();
            VirtualizationConnectorEntityMgr.fromEntity(entity, dto);
            if (request.isApi()) {
                VirtualizationConnectorDto.sanitizeVirtualizationConnector(dto);
            }
            res.setDto((R) dto);
        } else if (entityName.equals("ApplianceManagerConnector")) {
            ApplianceManagerConnector entity = getEntity(entityId, entityName, ApplianceManagerConnector.class, session);
            ApplianceManagerConnectorDto dto = new ApplianceManagerConnectorDto();
            ApplianceManagerConnectorEntityMgr.fromEntity(entity, dto);
            if (request.isApi()) {
                ApplianceManagerConnectorDto.sanitizeManagerConnector(dto);
            }

            boolean isPolicyMappingSupported =
                    ManagerApiFactory.createApplianceManagerApi(entity.getManagerType()).isPolicyMappingSupported();

            dto.setPolicyMappingSupported(isPolicyMappingSupported);

            res.setDto((R) dto);
        } else if (entityName.equals("Appliance")) {
            Appliance entity = getEntity(entityId, entityName, Appliance.class, session);
            ApplianceDto dto = new ApplianceDto();
            ApplianceEntityMgr.fromEntity(entity, dto);
            res.setDto((R) dto);
        } else if (entityName.equals("ApplianceSoftwareVersion")) {
            ApplianceSoftwareVersion entity = getEntity(entityId, entityName, ApplianceSoftwareVersion.class, session);
            ApplianceSoftwareVersionDto dto = new ApplianceSoftwareVersionDto();
            ApplianceSoftwareVersionEntityMgr.fromEntity(entity, dto);
            res.setDto((R) dto);
        } else if (entityName.equals("DistributedAppliance")) {
            DistributedAppliance entity = getEntity(entityId, entityName, DistributedAppliance.class, session);
            DistributedApplianceDto dto = new DistributedApplianceDto();
            DistributedApplianceEntityMgr.fromEntity(entity, dto);
            if (request.isApi()) {
                DistributedApplianceDto.sanitizeDistributedAppliance(dto);
            }
            res.setDto((R) dto);
        } else if (entityName.equals("User")) {
            User entity = getEntity(entityId, entityName, User.class, session);
            UserDto dto = new UserDto();
            UserEntityMgr.fromEntity(entity, dto);
            res.setDto((R) dto);
        } else if (entityName.equals("DistributedApplianceInstance")) {
            DistributedApplianceInstance entity = getEntity(entityId, entityName, DistributedApplianceInstance.class,
                    session);
            DistributedApplianceInstanceDto dto = new DistributedApplianceInstanceDto(entity);
            res.setDto((R) dto);
        } else if (entityName.equals("VirtualSystem")) {
            VirtualSystem entity = getEntity(entityId, entityName, VirtualSystem.class, session);
            VirtualSystemDto dto = new VirtualSystemDto();
            VirtualSystemEntityMgr.fromEntity(entity, dto);
            res.setDto((R) dto);
        } else if (entityName.equals("DeploymentSpec")) {
            DeploymentSpec entity = getEntity(entityId, entityName, DeploymentSpec.class, session);
            DeploymentSpecDto dto = new DeploymentSpecDto();
            DeploymentSpecEntityMgr.fromEntity(entity, dto);
            res.setDto((R) dto);
        } else if (entityName.equals("AvailabilityZone")) {
            AvailabilityZone entity = getEntity(entityId, entityName, AvailabilityZone.class, session);
            AvailabilityZoneDto dto = new AvailabilityZoneDto();
            AvailabilityZoneEntityMgr.fromEntity(entity, dto);
            res.setDto((R) dto);
        } else if (entityName.equals("SecurityGroupInterface")) {
            SecurityGroupInterface entity = getEntity(entityId, entityName, SecurityGroupInterface.class, session);
            SecurityGroupInterfaceDto dto = new SecurityGroupInterfaceDto();
            SecurityGroupInterfaceEntityMgr.fromEntity(entity, dto);
            res.setDto((R) dto);
        } else if (entityName.equals("SecurityGroup")) {
            SecurityGroup entity = getEntity(entityId, entityName, SecurityGroup.class, session);
            SecurityGroupDto dto = new SecurityGroupDto();
            SecurityGroupEntityMgr.fromEntity(entity, dto);
            SecurityGroupEntityMgr.generateDescription(session, dto);
            res.setDto((R) dto);
        } else if (entityName.equals("Alarm")) {
            Alarm entity = getEntity(entityId, entityName, Alarm.class, session);
            AlarmDto dto = new AlarmDto();
            AlarmEntityMgr.fromEntity(entity, dto);
            res.setDto((R) dto);
        } else if (entityName.equals("Alert")) {
            Alert entity = getEntity(entityId, entityName, Alert.class, session);
            AlertDto dto = new AlertDto();
            AlertEntityMgr.fromEntity(entity, dto);
            res.setDto((R) dto);
        }
        return res;
    }

    private <T extends IscEntity> T getEntity(Long entityId, String entityName, Class<T> clazz, Session session)
            throws VmidcBrokerValidationException {
        EntityManager<T> emgr = new EntityManager<T>(clazz, session);
        T entity = emgr.findByPrimaryKey(entityId);
        if (entity == null) {
            throw new VmidcBrokerValidationException(entityName + " entry with ID " + entityId + " is not found.");
        }
        return entity;
    }
}
