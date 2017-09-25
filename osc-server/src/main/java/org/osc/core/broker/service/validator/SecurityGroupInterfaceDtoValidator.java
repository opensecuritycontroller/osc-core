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

import org.osc.core.broker.service.dto.SecurityGroupInterfaceDto;
import org.osc.core.broker.util.ValidateUtil;

public class SecurityGroupInterfaceDtoValidator {

    public static void checkForNullFields(SecurityGroupInterfaceDto dto) throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<>();

        map.put("Name", dto.getName());
        map.put("Policy", dto.getPolicyIds());
        map.put("Tag", dto.getTagValue());
        map.put("Failure Policy", dto.getFailurePolicyType());

        ValidateUtil.checkForNullFields(map);
    }

    public static void checkFieldLength(SecurityGroupInterfaceDto dto) throws Exception {

        Map<String, String> map = new HashMap<>();

        map.put("Name", dto.getName());

        ValidateUtil.validateFieldLength(map, ValidateUtil.DEFAULT_MAX_LEN);
    }

}
