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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.osc.core.broker.model.entities.virtualization.openstack.HostAggregate;
import org.osc.core.broker.service.dto.openstack.HostAggregateDto;

public class HostAggregateEntityMgr {

    public static void fromEntity(HostAggregate entity, HostAggregateDto dto) {
        dto.setId(entity.getId());
        dto.setOpenstackId(entity.getOpenstackId());
        dto.setName(entity.getName());
    }

    public static void toEntity(HostAggregate entity, HostAggregateDto dto) {
        entity.setName(dto.getName());
    }

    public static List<HostAggregate> listByOpenstackId(EntityManager em, String openstackId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<HostAggregate> query = cb.createQuery(HostAggregate.class);

        Root<HostAggregate> root = query.from(HostAggregate.class);

        query = query.select(root).distinct(true)
            .where(cb.equal(root.get("openstackId"), openstackId));

        return em.createQuery(query).getResultList();
    }
}
