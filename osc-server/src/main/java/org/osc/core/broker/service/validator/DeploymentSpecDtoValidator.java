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

import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.util.ValidateUtil;

public class DeploymentSpecDtoValidator {


    public static void checkForNullFields(DeploymentSpecDto dto) throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("Name", dto.getName());

        map.put("Tenant Name", dto.getTenantName());
        map.put("Tenant", dto.getTenantId());

        map.put("Region", dto.getRegion());

        map.put("Virtual System Id", dto.getParentId());

        map.put("Management Network Name", dto.getManagementNetworkName());
        map.put("Management Network Id", dto.getManagementNetworkId());

        map.put("Inspection Network Name", dto.getInspectionNetworkName());
        map.put("Inspection Network Id", dto.getInspectionNetworkId());

        map.put("Instance Count", dto.getCount());

        ValidateUtil.checkForNullFields(map);
    }

    public static void checkFieldLength(DeploymentSpecDto dto) throws Exception {

        Map<String, String> map = new HashMap<String, String>();

        map.put("Name", dto.getName());
        ValidateUtil.validateFieldLength(map, ValidateUtil.DEFAULT_MAX_LEN);
    }
}