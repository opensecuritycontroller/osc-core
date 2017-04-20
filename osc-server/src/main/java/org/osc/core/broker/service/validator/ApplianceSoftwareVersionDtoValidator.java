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

import org.osc.core.broker.service.dto.ApplianceSoftwareVersionDto;
import org.osc.core.broker.util.ValidateUtil;

public class ApplianceSoftwareVersionDtoValidator {

    public static void checkForNullFields(ApplianceSoftwareVersionDto dto) throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("Appliance Id", dto.getParentId());
        map.put("Appliance Software Version", dto.getSwVersion());
        map.put("Virtualization Type", dto.getVirtualizationType());
        map.put("Virtualization Version", dto.getVirtualizationVersion());
        map.put("Image Url", dto.getImageUrl());

        ValidateUtil.checkForNullFields(map);
    }

    public static void checkFieldLength(ApplianceSoftwareVersionDto dto) throws Exception {

        Map<String, String> map = new HashMap<String, String>();

        map.put("Appliance Software Version", dto.getSwVersion());
        map.put("Virtualization Version", dto.getVirtualizationVersion());
        map.put("Image Url", dto.getImageUrl());

        ValidateUtil.validateFieldLength(map, ValidateUtil.DEFAULT_MAX_LEN);
    }
}