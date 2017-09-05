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
package org.osc.core.broker.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.request.DryRunRequest;
import org.osc.core.broker.service.request.VirtualizationConnectorRequest;
import org.osc.core.common.virtualization.VirtualizationType;

class VirtualizationConnectorUtilTestData {
    static DryRunRequest<VirtualizationConnectorRequest> generateOpenStackVCWithSDN() {
        return createOpenStackRequest(
                VirtualizationType.OPENSTACK, UUID.randomUUID().toString(), "NSC", null, null, null, "2.2.2.2", "provider user",
                "provider Password", "4.3", "Project Demo", "default", "RabbitMq User", "RabbitMq Password", "1111");
    }

    static DryRunRequest<VirtualizationConnectorRequest> generateK8sVCWithSDN() {
        return createOpenStackRequest(
                VirtualizationType.KUBERNETES, UUID.randomUUID().toString(), "NSC", null, null, null, "2.2.2.2", "provider user",
                "provider Password", "4.3", "Project Demo", "default", "RabbitMq User", "RabbitMq Password", "1111");
    }

    private static DryRunRequest<VirtualizationConnectorRequest> createOpenStackRequest(
            VirtualizationType virtualizationType,
            String name,
            String controllerType,
            String controllerIp,
            String controllerUser,
            String controllerPassword,
            String providerIp,
            String providerUser,
            String providerPassword,
            String version,
            String projectName,
            String domainId,
            String rabbitMqUser,
            String rabbitMqPassword,
            String rabbitMqPort
            ) {
        DryRunRequest<VirtualizationConnectorRequest> request = createRequest(virtualizationType, name, controllerType, controllerIp,
                controllerUser, controllerPassword, providerIp, providerUser, providerPassword, version);
        VirtualizationConnectorDto dto = request.getDto();

        dto.setAdminDomainId(domainId);
        dto.setAdminProjectName(projectName);

        Map<String, String> providerAttributes = new HashMap<>();
        providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER, rabbitMqUser);
        providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD, rabbitMqPassword);
        providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_PORT, rabbitMqPort);
        dto.setProviderAttributes(providerAttributes);

        return request;
    }

    private static DryRunRequest<VirtualizationConnectorRequest> createRequest(
            VirtualizationType virtualizationType,
            String name,
            String controllerType,
            String controllerIp,
            String controllerUser,
            String controllerPassword,
            String providerIp,
            String providerUser,
            String providerPassword,
            String version) {

        DryRunRequest<VirtualizationConnectorRequest> request = new DryRunRequest<>();

        request.setDto(createVCRequest(
                virtualizationType,
                name,
                controllerType,
                controllerIp,
                controllerUser,
                controllerPassword,
                providerIp,
                providerUser,
                providerPassword,
                version));

        return request;
    }

    private static VirtualizationConnectorRequest createVCRequest(VirtualizationType virtualizationType,
            String name,
            String controllerType,
            String controllerIp,
            String controllerUser,
            String controllerPassword,
            String providerIp,
            String providerUser,
            String providerPassword,
            String version){

        VirtualizationConnectorRequest request = new VirtualizationConnectorRequest();
        request.setType(virtualizationType);
        request.setName(name);
        request.setControllerType(controllerType);
        request.setControllerIP(controllerIp);
        request.setControllerUser(controllerUser);
        request.setControllerPassword(controllerPassword);
        request.setProviderIP(providerIp);
        request.setProviderUser(providerUser);
        request.setProviderPassword(providerPassword);
        request.setSoftwareVersion(version);

        return request;
    }
}
