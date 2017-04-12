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
import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.model.virtualization.OpenstackSoftwareVersion;
import org.osc.core.broker.model.virtualization.VmwareSoftwareVersion;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.util.ValidateUtil;

public class VirtualizationConnectorDtoValidator
		implements DtoValidator<VirtualizationConnectorDto, VirtualizationConnector> {

	private EntityManager em;
	private static final Logger LOG = Logger.getLogger(VirtualizationConnectorDtoValidator.class);

	public VirtualizationConnectorDtoValidator(EntityManager em) {
		this.em = em;
	}

	@Override
	public void validateForCreate(VirtualizationConnectorDto dto) throws Exception {
		// Initializing Entity Manager
        OSCEntityManager<VirtualizationConnector> emgr = new OSCEntityManager<>(
                VirtualizationConnector.class, this.em);

        VirtualizationConnectorDtoValidator.checkForNullFields(dto);
        VirtualizationConnectorDtoValidator.checkFieldLength(dto);

        // TODO: Future. Right now we assume Icehouse and 5.5 regardless of the actual version passed in, need to
        // fix this later.
        if (dto.getType().isOpenstack()) {
            dto.setSoftwareVersion(OpenstackSoftwareVersion.OS_ICEHOUSE.toString());
        } else if (dto.getType().isVmware()) {
            dto.setSoftwareVersion(VmwareSoftwareVersion.VMWARE_V5_5.toString());
        }

        // check for uniqueness of vc name
        if (emgr.isExisting("name", dto.getName())) {

            throw new VmidcBrokerValidationException(
                    "Virtualization Connector Name: " + dto.getName() + " already exists.");
        }

        // check for valid IP address format
        if (!dto.getType().isOpenstack()) {
            ValidateUtil.checkForValidIpAddressFormat(dto.getControllerIP());

            // check for uniqueness of vc nsx IP
            if (emgr.isExisting("controllerIpAddress", dto.getControllerIP())) {

                throw new VmidcBrokerValidationException(
                        "Controller IP Address: " + dto.getControllerIP() + " already exists.");
            }
        }

        VirtualizationConnectorDtoValidator.checkFieldFormat(dto);

        // check for uniqueness of vc vCenter IP
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
    public static void checkForNullFields(VirtualizationConnectorDto dto, boolean skipPasswordNullCheck)
            throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> notNullFieldsMap = new HashMap<String, Object>();
        Map<String, Object> nullFieldsMap = new HashMap<String, Object>();

        notNullFieldsMap.put("Name", dto.getName());
        notNullFieldsMap.put("Type", dto.getType());
        ValidateUtil.checkForNullFields(notNullFieldsMap);

        if (dto.getType().isVmware()) {
            notNullFieldsMap.put("Controller IP Address", dto.getControllerIP());
            notNullFieldsMap.put("Controller User Name", dto.getControllerUser());
            if (!skipPasswordNullCheck) {
                notNullFieldsMap.put("Controller Password", dto.getControllerPassword());
            }
            nullFieldsMap.put("Rabbit MQ User",
                    dto.getProviderAttributes().get(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER));
            nullFieldsMap.put("Rabbit MQ Password",
                    dto.getProviderAttributes().get(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD));
            nullFieldsMap.put("Rabbit MQ Port",
                    dto.getProviderAttributes().get(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_PORT));
            nullFieldsMap.put("Admin Tenant", dto.getAdminTenantName());

        } else if (dto.getType().isOpenstack()) {
            notNullFieldsMap.put("Admin Tenant Name", dto.getAdminTenantName());
            if (!dto.isControllerDefined()) {
                nullFieldsMap.put("Controller IP Address", dto.getControllerIP());
                nullFieldsMap.put("Controller User Name", dto.getControllerUser());
                nullFieldsMap.put("Controller Password", dto.getControllerPassword());
            } else {
                if (!SdnControllerApiFactory.usesProviderCreds(dto.getControllerType())) {
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

    public static void checkForNullFields(VirtualizationConnectorDto dto) throws Exception {
        checkForNullFields(dto, false);
    }

    public static void checkFieldLength(VirtualizationConnectorDto dto) throws Exception {

        Map<String, String> map = new HashMap<String, String>();

        map.put("Name", dto.getName());

        map.put("Controller User Name", dto.getControllerUser());
        map.put("Controller Password", dto.getControllerPassword());

        map.put("Provider User Name", dto.getProviderUser());
        map.put("Provider Password", dto.getProviderPassword());
        if (dto.getType().isOpenstack()) {
            map.put("Admin Tenant Name", dto.getAdminTenantName());
            String rabbitMqPort = dto.getProviderAttributes().get(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_PORT);
            if (!StringUtils.isNumeric(rabbitMqPort)) {
                throw new VmidcBrokerInvalidEntryException(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_PORT
                        + " expected to be an Integer. Value is: " + rabbitMqPort);
            }
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
