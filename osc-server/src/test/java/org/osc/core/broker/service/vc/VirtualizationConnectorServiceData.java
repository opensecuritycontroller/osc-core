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
import org.osc.core.common.controller.ControllerType;
import org.osc.core.common.virtualization.VirtualizationType;

public class VirtualizationConnectorServiceData {

    public static String OPENSTACK_NAME_ALREADY_EXISTS = "Openstack Name";
    public static String CONTROLLER_IP_ALREADY_EXISTS = "127.0.0.1";
    public static String PROVIDER_IP_ALREADY_EXISTS = "127.0.0.2";
    public static String CONTROLLER_IP_ALREADY_EXISTS_2 = "127.0.0.3";
    public static String PROVIDER_IP_ALREADY_EXISTS_2 = "127.0.0.4";

    public static VirtualizationConnector createVirtualisationConnector(String name,
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

    public static DryRunRequest<VirtualizationConnectorRequest> OPENSTACK_NAME_ALREADY_EXISTS_NOCONTROLLER_REQUEST = createOpenStackRequest(
            VirtualizationType.OPENSTACK, OPENSTACK_NAME_ALREADY_EXISTS, null, null, null, "2.2.2.2", "provider user",
            "provider Password", "4.3", "Tenant Demo", "default", "RabbitMq User", "RabbitMq Password", "1111", null);

    public static DryRunRequest<VirtualizationConnectorRequest> OPENSTACK_NAME_ALREADY_EXISTS_NSC_REQUEST = createOpenStackRequest(
            VirtualizationType.OPENSTACK, OPENSTACK_NAME_ALREADY_EXISTS, null, null, null, "2.2.2.2", "provider user",
            "provider Password", "4.3", "Tenant Demo", "default", "RabbitMq User", "RabbitMq Password", "1111", "NSC");

    public static DryRunRequest<VirtualizationConnectorRequest> PROVIDER_IP_ALREADY_EXISTS_OPENSTACK_REQUEST = createOpenStackRequest(
            VirtualizationType.OPENSTACK, "Random Openstack name", "1.1.1.1", "SDNUserName", "SDNPwd", PROVIDER_IP_ALREADY_EXISTS,
            "provider user", "provider Password", "4.3", "Tenant Demo", "default", "RabbitMq User", "RabbitMq Password", "1111",
            "NSC");

    public static DryRunRequest<VirtualizationConnectorRequest> OPENSTACK_NSC_REQUEST = createOpenStackRequest(
            VirtualizationType.OPENSTACK, "Random Openstack name", null, null, null, "2.2.2.2", "provider user",
            "provider Password", "4.3", "Tenant Demo", "default", "RabbitMq User", "RabbitMq Password", "1111", "NSC");

    public static DryRunRequest<VirtualizationConnectorRequest> OPENSTACK_CONTROLLER_IP_ALREADY_EXISTS_REQUEST = createOpenStackRequest(
            VirtualizationType.OPENSTACK, "Random Openstack name", CONTROLLER_IP_ALREADY_EXISTS, "SDNUserName", "SDNPwd", "2.2.2.2", "provider user",
            "provider Password", "4.3", "Tenant Demo", "default", "RabbitMq User", "RabbitMq Password", "1111", "NSC");

    public static DryRunRequest<VirtualizationConnectorRequest> OPENSTACK_NOCONTROLLER_REQUEST = createOpenStackRequest(
            VirtualizationType.OPENSTACK, "Random Openstack name", null, null, null, "2.2.2.2", "provider user",
            "provider Password", "4.3", "Tenant Demo", "default", "RabbitMq User", "RabbitMq Password", "1111", null);

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

    public static VirtualizationConnectorDto getVCDtoforOpenStack(){

        VirtualizationConnectorDto vcDto = getVCDto(VirtualizationType.OPENSTACK, "Random Openstack name", null, null, null, "2.2.2.2", "provider user",
                "provider Password", "4.3");
        setOpenStackParams(vcDto, "Tenant Demo", "default", "RabbitMq User", "RabbitMq Password", "1111", null);

        return vcDto;
    }

    public static VirtualizationConnectorDto getVCDtoforOpenStackwithSDN(){

        VirtualizationConnectorDto vcDto = getVCDto(VirtualizationType.OPENSTACK, "Random Openstack name", "1.1.1.1", "controller user", "controller password", "2.2.2.2", "provider user",
                "provider Password", "4.3");
        setOpenStackParams(vcDto, "Tenant Demo", "default", "RabbitMq User", "RabbitMq Password", "1111", "NSC");

        return vcDto;
    }

    private static void setOpenStackParams(VirtualizationConnectorDto vcDto,
            String tenantName,
            String domainId,
            String rabbitMquser,
            String rabbitMqpassword,
            String rabbitMqport,
            String controllerTypeStr){

        vcDto.setAdminDomainId(domainId);
        vcDto.setAdminTenantName(tenantName);

        Map<String, String> providerAttributes = new HashMap<>();
        providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER, rabbitMquser);
        providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD, rabbitMqpassword);
        providerAttributes.put(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_PORT, rabbitMqport);
        vcDto.setProviderAttributes(providerAttributes);

        if (controllerTypeStr != null && (!controllerTypeStr.isEmpty())) {
            ControllerType.addType(controllerTypeStr);
            vcDto.setControllerType(ControllerType.fromText(controllerTypeStr));
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
            String tenantName,
            String domainId,
            String rabbitMquser,
            String rabbitMqpassword,
            String rabbitMqport,
            String controllerTypeStr) {

        DryRunRequest<VirtualizationConnectorRequest> request = createRequest(virtualizationType, name, controllerIp,
                controllerUser, controllerPassword, providerIp, providerUser, providerPassword, version);
        VirtualizationConnectorDto dto = request.getDto();
        setOpenStackParams(dto, tenantName, domainId, rabbitMquser, rabbitMqpassword, rabbitMqport, controllerTypeStr);

        return request;
    }

    public static DryRunRequest<VirtualizationConnectorDto> getOpenStackRequestwithSDN() {

        DryRunRequest<VirtualizationConnectorDto> request = new DryRunRequest<>();
        request.setDto(getVCDtoforOpenStackwithSDN());

        return request;
    }

}