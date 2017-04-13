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
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.service.dto.ApplianceDto;



public class ApplianceEntityMgr {

    public static Appliance createEntity(// for add
            ApplianceDto dto) {
        Appliance a = new Appliance();

        toEntity(a, dto);

        return a;

    }

    public static void toEntity(Appliance a, ApplianceDto dto) {

        // transform from dto to entity
        a.setId(dto.getId());
        a.setModel(dto.getModel());
        a.setManagerType(dto.getManagerType().getValue());
        a.setManagerSoftwareVersion(dto.getManagerVersion());
    }

    public static void fromEntity(Appliance a, ApplianceDto dto) {

        // transform from entity to dto
        dto.setId(a.getId());
        dto.setModel(a.getModel());
        dto.setManagerType(ManagerType.fromText(a.getManagerType().toString()));
        dto.setManagerVersion(a.getManagerSoftwareVersion().toString());
    }

    public static Appliance findById(EntityManager em, Long id) {

        // Initializing Entity Manager
        OSCEntityManager<Appliance> emgr = new OSCEntityManager<Appliance>(Appliance.class, em);

        return emgr.findByPrimaryKey(id);
    }

    public static Appliance findByModel(EntityManager em, String model) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Appliance> query = cb.createQuery(Appliance.class);

        Root<Appliance> root = query.from(Appliance.class);

        query = query.select(root)
            .where(cb.equal(root.get("model"), model));

        try {
            return em.createQuery(query).getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }
}
