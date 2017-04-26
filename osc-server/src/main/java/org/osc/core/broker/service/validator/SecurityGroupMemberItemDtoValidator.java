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

import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.service.dto.SecurityGroupMemberItemDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.util.ValidateUtil;

public class SecurityGroupMemberItemDtoValidator {

    public static void checkForNullFields(SecurityGroupMemberItemDto dto) throws VmidcBrokerInvalidEntryException {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("Name", dto.getName());
        map.put("Region", dto.getRegion());
        map.put("Openstack Id", dto.getOpenstackId());
        map.put("Type", dto.getType());

        if (dto.getType().equals(SecurityGroupMemberType.SUBNET)) {
            map.put("Network  Id", dto.getParentOpenStackId());
        }

        ValidateUtil.checkForNullFields(map);
    }

}
