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
package org.osc.core.broker.service.validator;

import java.util.HashMap;
import java.util.Map;

import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.request.VirtualizationConnectorRequest;
import org.osc.core.common.virtualization.VirtualizationType;

public class VirtualizationConnectorDtoValidatorTestData {
    static String OPENSTACK_NAME_ALREADY_EXISTS = "Openstack Name";
    static String PROVIDER_IP_ALREADY_EXISTS = "127.0.0.2";
    static String CONTROLLER_IP_ALREADY_EXISTS = "127.0.0.1";

    static VirtualizationConnectorDto OPENSTACK_NOCONTROLLER_VC = createOpenStackVCDto(
            VirtualizationType.OPENSTACK, "Random Openstack name", null, null, null, null, "2.2.2.2", "provider user",
            "provider Password", "4.3", "Project Demo", "default", "RabbitMq User", "RabbitMq Password", "1111");

    static VirtualizationConnectorDto OPENSTACK_NAME_ALREADY_EXISTS_NOCONTROLLER_VC = createOpenStackVCDto(
            VirtualizationType.OPENSTACK, OPENSTACK_NAME_ALREADY_EXISTS, null, null, null, null, "2.2.2.2", "provider user",
            "provider Password", "4.3", "Project Demo", "default", "RabbitMq User", "RabbitMq Password", "1111");

    static VirtualizationConnectorDto PROVIDER_IP_ALREADY_EXISTS_OPENSTACK_VC = createOpenStackVCDto(
            VirtualizationType.OPENSTACK, "Random Openstack name", "NSC", "1.1.1.1", "SDNUserName", "SDNPwd", PROVIDER_IP_ALREADY_EXISTS,
            "provider user", "provider Password", "4.3", "Project Demo", "default", "RabbitMq User", "RabbitMq Password", "1111");

    static VirtualizationConnectorDto OPENSTACK_CONTROLLER_IP_ALREADY_EXISTS_VC = createOpenStackVCDto(
            VirtualizationType.OPENSTACK, "Random Openstack name", "NSC", CONTROLLER_IP_ALREADY_EXISTS, "SDNUserName", "SDNPwd", "2.2.2.2", "provider user",
            "provider Password", "4.3", "Project Demo", "default", "RabbitMq User", "RabbitMq Password", "1111");

    static VirtualizationConnector createVirtualizationConnector(String name,
            String controller, String provider) {
        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setName(name);
        vc.setControllerIpAddress(controller);
        vc.setProviderIpAddress(provider);
        vc.setVirtualizationSoftwareVersion("vcSoftwareVersion");
        vc.setProviderUsername("Natasha");
        vc.setProviderPassword("********");
        vc.setVirtualizationType(VirtualizationType.OPENSTACK);
        return vc;
    }

    static VirtualizationConnectorDto generateOpenStackVCWithoutSDN(){
        return createOpenStackVCDto(VirtualizationType.OPENSTACK, "Random Openstack name", null, null, null, null, "2.2.2.2", "provider user",
                "provider Password", "4.3", "Project Demo", "default", "RabbitMq User", "RabbitMq Password", "1111");
    }

    static VirtualizationConnectorDto generateK8skVCWithoutSDN(){
        return createOpenStackVCDto(VirtualizationType.KUBERNETES, "Random Openstack name", null, null, null, null, "2.2.2.2", "provider user",
                "provider Password", "4.3", "Project Demo", "default", "RabbitMq User", "RabbitMq Password", "1111");
    }

    static VirtualizationConnectorDto generateK8sVCWithoSDNNone(){
        return createOpenStackVCDto(VirtualizationType.KUBERNETES, "Random Openstack name", "NONE", null, null, null, "2.2.2.2", "provider user",
                "provider Password", "4.3", "Project Demo", "default", "RabbitMq User", "RabbitMq Password", "1111");
    }

    public static VirtualizationConnectorDto generateOpenStackVCWithSDN(){
        return createOpenStackVCDto(VirtualizationType.OPENSTACK, "Random Openstack name", "NSC", "1.1.1.1", "controller user", "controller password", "2.2.2.2", "provider user",
                "provider Password", "4.3", "Project Demo", "default", "RabbitMq User", "RabbitMq Password", "1111");
    }

    private static VirtualizationConnectorDto createOpenStackVCDto(
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

        VirtualizationConnectorDto dto = createVCDto(virtualizationType, name, controllerType, controllerIp,
                controllerUser, controllerPassword, providerIp, providerUser, providerPassword, version);


        dto.setAdminDomainId(domainId);
        dto.setAdminProjectName(projectName);

        Map<String, String> providerAttributes = new HashMap<>();
        providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER, rabbitMqUser);
        providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD, rabbitMqPassword);
        providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_PORT, rabbitMqPort);
        dto.setProviderAttributes(providerAttributes);

        return dto;
    }

    private static VirtualizationConnectorDto createVCDto(VirtualizationType virtualizationType,
            String name,
            String controllerType,
            String controllerIp,
            String controllerUser,
            String controllerPassword,
            String providerIp,
            String providerUser,
            String providerPassword,
            String version){

        VirtualizationConnectorDto dto = new VirtualizationConnectorRequest();
        dto.setType(virtualizationType);
        dto.setName(name);
        dto.setControllerType(controllerType);
        dto.setControllerIP(controllerIp);
        dto.setControllerUser(controllerUser);
        dto.setControllerPassword(controllerPassword);
        dto.setProviderIP(providerIp);
        dto.setProviderUser(providerUser);
        dto.setProviderPassword(providerPassword);
        dto.setSoftwareVersion(version);

        return dto;
    }
}
