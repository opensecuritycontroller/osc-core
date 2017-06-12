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

import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;

public class SubnetEntityManager {

    public static Subnet findByOpenstackId(EntityManager em, String id) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Subnet> query = cb.createQuery(Subnet.class);

        Root<Subnet> root = query.from(Subnet.class);

        query = query.select(root)
            .where(cb.equal(root.get("openstackId"), id));

        try {
            return em.createQuery(query).getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    public static Subnet findById(EntityManager em, Long id) {
        return em.find(Subnet.class, id);
    }
}
