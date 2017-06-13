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

import org.osc.core.broker.service.dto.ApplianceManagerConnectorDto;
import org.osc.core.broker.util.ValidateUtil;

public class ApplianceManagerConnectorDtoValidator {

    /**
     * Based on the type of DTO makes sure the required fields are not null and the fields which should
     * not be specified for the type are null.
     *
     * @param dto
     *            the dto
     * @throws Exception
     *             in case the required fields are null or fields which should
     *             NOT be specified are specified
     */
    public static void checkForNullFields(ApplianceManagerConnectorDto dto, boolean skipPasswordNullCheck,
            boolean isBasicAuth, boolean isKeyAuth) throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> notNullFieldsMap = new HashMap<String, Object>();
        Map<String, Object> nullFieldsMap = new HashMap<String, Object>();

        notNullFieldsMap.put("Name", dto.getName());
        notNullFieldsMap.put("Type", dto.getManagerType());
        notNullFieldsMap.put("IP Address", dto.getIpAddress());

        if (isKeyAuth && !skipPasswordNullCheck) {
            notNullFieldsMap.put("API Key", dto.getApiKey());
        } else if (isBasicAuth) {
            if (!skipPasswordNullCheck) {
                notNullFieldsMap.put("Password", dto.getPassword());
            }
            notNullFieldsMap.put("User Name", dto.getUsername());

            nullFieldsMap.put("API Key", dto.getApiKey());
        }
        ValidateUtil.checkForNullFields(notNullFieldsMap);
        ValidateUtil.validateFieldsAreNull(nullFieldsMap);
    }

    public static void checkForNullFields(ApplianceManagerConnectorDto dto, boolean isBasicAuth, boolean isKeyAuth) throws Exception {
        checkForNullFields(dto, false, isBasicAuth, isKeyAuth);
    }

    public static void checkFieldLength(ApplianceManagerConnectorDto dto, boolean isBasicAuth) throws Exception {

        Map<String, String> map = new HashMap<String, String>();

        map.put("Name", dto.getName());
        if (isBasicAuth) {
            map.put("Password", dto.getPassword());
            map.put("User Name", dto.getUsername());
        }
        ValidateUtil.validateFieldLength(map, ValidateUtil.DEFAULT_MAX_LEN);
    }
}
