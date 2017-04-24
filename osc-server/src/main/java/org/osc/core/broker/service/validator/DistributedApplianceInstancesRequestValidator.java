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

import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.request.DistributedApplianceInstancesRequest;
import org.osc.core.broker.util.ValidateUtil;

public class DistributedApplianceInstancesRequestValidator {

    public static void checkForNullFields(DistributedApplianceInstancesRequest request) throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> notNullFieldsMap = new HashMap<String, Object>();

        notNullFieldsMap.put("dtoIdList", request.getDtoIdList());

        for (Long id : request.getDtoIdList()) {
            if (id == null) {
                notNullFieldsMap.put("dtoId", id);
            }
        }
        ValidateUtil.checkForNullFields(notNullFieldsMap);

        if(request.getDtoIdList().isEmpty()) {
            throw new VmidcBrokerInvalidEntryException("dtoIdList should not be empty.");
        }
    }

}
