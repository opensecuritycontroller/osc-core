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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.service.dto.ServiceFunctionChainDto;
import org.osc.core.broker.service.dto.VirtualSystemDto;

public class ServiceFunctionChainEntityMgr {

    public static void fromEntity(ServiceFunctionChain sfcEntity, ServiceFunctionChainDto dto) {
        dto.setName(sfcEntity.getName());
        dto.setId(sfcEntity.getId());
        dto.setParentId(sfcEntity.getVirtualizationConnector().getId());
        List<VirtualSystemDto> vsDtoList = new ArrayList<VirtualSystemDto>();

        for (VirtualSystem vs : sfcEntity.getVirtualSystems()) {
            VirtualSystemDto vsDto = new VirtualSystemDto();
            VirtualSystemEntityMgr.fromEntity(vs, vsDto);
            vsDtoList.add(vsDto);
        }
        dto.setVirtualSystemDto(vsDtoList);
    }

    public static ServiceFunctionChain findById(EntityManager em, Long id) {
        return em.find(ServiceFunctionChain.class, id);
    }

    public static List<ServiceFunctionChain> listServiceFunctionChainsByVirtualSystem(EntityManager em, VirtualSystem vs) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<ServiceFunctionChain> query = cb.createQuery(ServiceFunctionChain.class);

        Root<ServiceFunctionChain> root = query.from(ServiceFunctionChain.class);
        query = query.select(root).where(cb.equal(root.join("virtualSystems"), vs));

        return em.createQuery(query).getResultList();
    }

}
