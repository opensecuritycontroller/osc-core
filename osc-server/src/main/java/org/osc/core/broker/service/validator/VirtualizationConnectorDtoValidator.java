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
import java.util.Map.Entry;

import javax.persistence.EntityManager;

import org.apache.commons.lang.StringUtils;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.common.virtualization.OpenstackSoftwareVersion;

public class VirtualizationConnectorDtoValidator
implements DtoValidator<VirtualizationConnectorDto, VirtualizationConnector> {

    private EntityManager em;
    private TransactionalBroadcastUtil txBroadcastUtil;
    private ApiFactoryService apiFactoryService;

    public VirtualizationConnectorDtoValidator(EntityManager em, TransactionalBroadcastUtil txBroadcastUtil, ApiFactoryService apiFactoryService) {
        this.em = em;
        this.txBroadcastUtil = txBroadcastUtil;
        this.apiFactoryService = apiFactoryService;
    }

    @Override
    public void validateForCreate(VirtualizationConnectorDto dto) throws Exception {
        // Initializing Entity Manager
        OSCEntityManager<VirtualizationConnector> emgr = new OSCEntityManager<>(
                VirtualizationConnector.class, this.em, this.txBroadcastUtil);

        boolean usesProviderCreds = dto.isControllerDefined() && this.apiFactoryService.usesProviderCreds(dto.getControllerType());
        VirtualizationConnectorDtoValidator.checkForNullFields(dto, usesProviderCreds);
        VirtualizationConnectorDtoValidator.checkFieldLength(dto);

        // TODO: Future. Right now we assume Icehouse and 5.5 regardless of the actual version passed in, need to
        // fix this later.
        if (dto.getType().isOpenstack()) {
            dto.setSoftwareVersion(OpenstackSoftwareVersion.OS_ICEHOUSE.toString());
        }

        // check for uniqueness of vc name
        if (emgr.isExisting("name", dto.getName())) {

            throw new VmidcBrokerValidationException(
                    "Virtualization Connector Name: " + dto.getName() + " already exists.");
        }

        // check for uniqueness of controller IP
        if (dto.isControllerDefined() && !this.apiFactoryService.usesProviderCreds(dto.getControllerType())) {
            ValidateUtil.checkForValidIpAddressFormat(dto.getControllerIP());
            if (emgr.isExisting("controllerIpAddress", dto.getControllerIP())) {

                throw new VmidcBrokerValidationException(
                        "Controller IP Address: " + dto.getControllerIP() + " already exists.");
            }
        }

        VirtualizationConnectorDtoValidator.checkFieldFormat(dto);

        // check for uniqueness of provider IP
        if (emgr.isExisting("providerIpAddress", dto.getProviderIP())) {

            throw new VmidcBrokerValidationException(
                    "Provider IP Address: " + dto.getProviderIP() + " already exists.");
        }


    }

    @Override
    public VirtualizationConnector validateForUpdate(VirtualizationConnectorDto dto) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * Based on the type of DTO makes sure the required fields are not null and the fields which should
     * not be specified for the type are null.
     *
     * @param dto
     *            the dto
     * @throws VmidcBrokerInvalidEntryException
     *             in case the required fields are null or fields which should
     *             NOT be specified are specified
     */
    public static void checkForNullFields(VirtualizationConnectorDto dto, boolean skipPasswordNullCheck, boolean usesProviderCreds)
            throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> notNullFieldsMap = new HashMap<String, Object>();
        Map<String, Object> nullFieldsMap = new HashMap<String, Object>();

        notNullFieldsMap.put("Name", dto.getName());
        notNullFieldsMap.put("Type", dto.getType());
        ValidateUtil.checkForNullFields(notNullFieldsMap);

        if (dto.getType().isOpenstack()) {
            notNullFieldsMap.put("Admin Project Name", dto.getAdminProjectName());
            notNullFieldsMap.put("Admin Domain Id", dto.getAdminDomainId());
            if (!dto.isControllerDefined()) {
                nullFieldsMap.put("Controller IP Address", dto.getControllerIP());
                nullFieldsMap.put("Controller User Name", dto.getControllerUser());
                nullFieldsMap.put("Controller Password", dto.getControllerPassword());
            } else {
                if (!usesProviderCreds) {
                    notNullFieldsMap.put("Controller IP Address", dto.getControllerIP());
                    notNullFieldsMap.put("Controller User Name", dto.getControllerUser());
                    if (!skipPasswordNullCheck) {
                        notNullFieldsMap.put("Controller Password", dto.getControllerPassword());
                    }
                } else {
                    nullFieldsMap.put("Controller IP Address", dto.getControllerIP());
                    nullFieldsMap.put("Controller User Name", dto.getControllerUser());
                    nullFieldsMap.put("Controller Password", dto.getControllerPassword());
                }
            }
            notNullFieldsMap.put("Rabbit MQ User",
                    dto.getProviderAttributes().get(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER));
            notNullFieldsMap.put("Rabbit MQ Password",
                    dto.getProviderAttributes().get(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD));
            notNullFieldsMap.put("Rabbit MQ Port",
                    dto.getProviderAttributes().get(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_PORT));
        }
        notNullFieldsMap.put("Provider IP Address", dto.getProviderIP());
        notNullFieldsMap.put("Provider User Name", dto.getProviderUser());
        if (!skipPasswordNullCheck) {
            notNullFieldsMap.put("Provider Password", dto.getProviderPassword());
        }

        notNullFieldsMap.put("Software Version", dto.getSoftwareVersion());

        ValidateUtil.checkForNullFields(notNullFieldsMap);
        ValidateUtil.validateFieldsAreNull(nullFieldsMap);
    }

    public static void checkForNullFields(VirtualizationConnectorDto dto, boolean usesProviderCreds) throws Exception {
        checkForNullFields(dto, false, usesProviderCreds);
    }

    public static void checkFieldLength(VirtualizationConnectorDto dto) throws Exception {

        Map<String, String> map = new HashMap<>();

        map.put("Name", dto.getName());

        map.put("Controller User Name", dto.getControllerUser());
        map.put("Controller Password", dto.getControllerPassword());

        map.put("Provider User Name", dto.getProviderUser());
        map.put("Provider Password", dto.getProviderPassword());
        if (dto.getType().isOpenstack()) {
            map.put("Admin Project Name", dto.getAdminProjectName());
            String rabbitMqPort = dto.getProviderAttributes().get(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_PORT);
            if (!StringUtils.isNumeric(rabbitMqPort)) {
                throw new VmidcBrokerInvalidEntryException(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_PORT
                        + " expected to be an Integer. Value is: " + rabbitMqPort);
            }
            map.put("Admin Domain Id", dto.getAdminDomainId());
        }
        if (dto.getProviderAttributes() != null) {
            for (Entry<String, String> entry : dto.getProviderAttributes().entrySet()) {
                map.put("Attribute Key", entry.getKey());
                map.put("Attribute Value", entry.getValue());
            }
        }

        ValidateUtil.validateFieldLength(map, ValidateUtil.DEFAULT_MAX_LEN);
    }

    public static void checkFieldFormat(VirtualizationConnectorDto dto) throws VmidcBrokerInvalidEntryException {
        ValidateUtil.checkForValidIpAddressFormat(dto.getProviderIP());

        if (dto.getType().isOpenstack() && dto.getProviderAttributes() != null) {
            String rabbitMqPort = dto.getProviderAttributes().get(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_PORT);
            if (!StringUtils.isNumeric(rabbitMqPort)) {
                throw new VmidcBrokerInvalidEntryException(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_PORT
                        + " expected to be an Integer. Value is: " + rabbitMqPort);
            }

            String rabbitMQIP = dto.getProviderAttributes().get(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_IP);
            if (!StringUtils.isBlank(rabbitMQIP)) {
                ValidateUtil.checkForValidIpAddressFormat(rabbitMQIP);
            }
        }
    }

}
