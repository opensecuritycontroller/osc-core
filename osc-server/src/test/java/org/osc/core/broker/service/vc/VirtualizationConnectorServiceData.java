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
package org.osc.core.broker.service.vc;

import java.util.HashMap;
import java.util.Map;

import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.VirtualizationConnectorRequest;
import org.osc.core.common.virtualization.VirtualizationType;

public class VirtualizationConnectorServiceData {
    static String OPENSTACK_NAME_ALREADY_EXISTS = "Openstack Name";

    static DryRunRequest<VirtualizationConnectorRequest> OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST = createOpenStackRequest(
            VirtualizationType.OPENSTACK, OPENSTACK_NAME_ALREADY_EXISTS, null, null, null, "2.2.2.2", "provider user",
            "provider Password", "4.3", "Project Demo", "default", "RabbitMq User", "RabbitMq Password", "1111", "NSC");


    static DryRunRequest<VirtualizationConnectorRequest> OPENSTACK_NOCONTROLLER_REQUEST = createOpenStackRequest(
            VirtualizationType.OPENSTACK, "Random Openstack name", null, null, null, "2.2.2.2", "provider user",
            "provider Password", "4.3", "Project Demo", "default", "RabbitMq User", "RabbitMq Password", "1111", null);

    private static DryRunRequest<VirtualizationConnectorRequest> createRequest(
            VirtualizationType virtualizationType,
            String name,
            String controllerIp,
            String controllerUser,
            String controllerPassword,
            String providerIp,
            String providerUser,
            String providerPassword,
            String version) {

        DryRunRequest<VirtualizationConnectorRequest> request = new DryRunRequest<>();
        VirtualizationConnectorRequest dto = getVCDto(virtualizationType, name, controllerIp, controllerUser,
                controllerPassword, providerIp, providerUser, providerPassword, version);
        request.setDto(dto);

        return request;
    }

    private static VirtualizationConnectorRequest getVCDto(VirtualizationType virtualizationType,
            String name,
            String controllerIp,
            String controllerUser,
            String controllerPassword,
            String providerIp,
            String providerUser,
            String providerPassword,
            String version){

        VirtualizationConnectorRequest dto = new VirtualizationConnectorRequest();
        dto.setType(virtualizationType);
        dto.setName(name);
        dto.setControllerIP(controllerIp);
        dto.setControllerUser(controllerUser);
        dto.setControllerPassword(controllerPassword);
        dto.setProviderIP(providerIp);
        dto.setProviderUser(providerUser);
        dto.setProviderPassword(providerPassword);
        dto.setSoftwareVersion(version);

        return dto;
    }

    private static void setOpenStackParams(VirtualizationConnectorDto vcDto,
            String projectName,
            String domainId,
            String rabbitMquser,
            String rabbitMqpassword,
            String rabbitMqport,
            String controllerTypeStr){

        vcDto.setAdminDomainId(domainId);
        vcDto.setAdminProjectName(projectName);

        Map<String, String> providerAttributes = new HashMap<>();
        providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER, rabbitMquser);
        providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD, rabbitMqpassword);
        providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_PORT, rabbitMqport);
        vcDto.setProviderAttributes(providerAttributes);

        if (controllerTypeStr != null && (!controllerTypeStr.isEmpty())) {
            vcDto.setControllerType(controllerTypeStr);
        }
    }

    private static DryRunRequest<VirtualizationConnectorRequest> createOpenStackRequest(
            VirtualizationType virtualizationType,
            String name,
            String controllerIp,
            String controllerUser,
            String controllerPassword,
            String providerIp,
            String providerUser,
            String providerPassword,
            String version,
            String projectName,
            String domainId,
            String rabbitMquser,
            String rabbitMqpassword,
            String rabbitMqport,
            String controllerTypeStr) {

        DryRunRequest<VirtualizationConnectorRequest> request = createRequest(virtualizationType, name, controllerIp,
                controllerUser, controllerPassword, providerIp, providerUser, providerPassword, version);
        VirtualizationConnectorDto dto = request.getDto();
        setOpenStackParams(dto, projectName, domainId, rabbitMquser, rabbitMqpassword, rabbitMqport, controllerTypeStr);

        return request;
    }
}