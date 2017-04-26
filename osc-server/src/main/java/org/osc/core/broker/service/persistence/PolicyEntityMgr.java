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
package org.osc.core.broker.service.persistence;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.service.dto.PolicyDto;

public class PolicyEntityMgr {

    public static void fromEntity(Policy entity, PolicyDto dto) {
        dto.setId(entity.getId());
        dto.setPolicyName(entity.getName());
        dto.setMgrPolicyId(entity.getMgrPolicyId());
        dto.setMgrDomainId(entity.getDomain().getId());
        dto.setMgrDomainName(entity.getDomain().getName());
    }

    public static Policy findById(EntityManager em, Long id) {

        // Initializing Entity Manager
        OSCEntityManager<Policy> emgr = new OSCEntityManager<Policy>(
                Policy.class, em);

        return emgr.findByPrimaryKey(id);
    }

}
