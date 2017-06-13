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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.virtualization.openstack.Host;
import org.osc.core.broker.service.dto.openstack.HostDto;

public class HostEntityMgr {
    public static void fromEntity(Host entity, HostDto dto) {
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setOpenstackId(entity.getOpenstackId());
    }

    public static void toEntity(Host entity, HostDto dto) {
        entity.setName(dto.getName());
        entity.setOpenstackId(dto.getOpenstackId());
    }

    public static Set<HostDto> fromEntity(Set<Host> hosts) {
        Set<HostDto> hostSet = new HashSet<HostDto>();
        if (hosts != null) {
            for (Host host : hosts) {
                HostDto hsDto = new HostDto();
                HostEntityMgr.fromEntity(host, hsDto);
                hostSet.add(hsDto);
            }
        }
        return hostSet;
    }


    public static Host findById(EntityManager em, Long id) {
        return em.find(Host.class, id);
    }
}
