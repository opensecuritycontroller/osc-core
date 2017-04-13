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

import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.sdk.sdn.element.ServiceProfileElement;

public class VirtualSystemPolicyEntityMgr {

    public static VirtualSystemPolicy findVirtualSystemPolicyByNsxId(EntityManager em, VirtualSystem vs,
            ServiceProfileElement serviceProfile) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<VirtualSystemPolicy> query = cb.createQuery(VirtualSystemPolicy.class);

        Root<VirtualSystemPolicy> root = query.from(VirtualSystemPolicy.class);

        query = query.select(root)
            .where(cb.equal(root.get("virtualSystem"), vs),
                   cb.equal(root.get("nsxVendorTemplateId"), serviceProfile.getVendorTemplateId()));

        List<VirtualSystemPolicy> list = em.createQuery(query).setMaxResults(1).getResultList();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public static List<VirtualSystemPolicy> listVSPolicyByPolicyId(EntityManager em, Long policyId) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<VirtualSystemPolicy> query = cb.createQuery(VirtualSystemPolicy.class);

        Root<VirtualSystemPolicy> root = query.from(VirtualSystemPolicy.class);

        query = query.select(root).distinct(true)
            .where(cb.equal(root.join("policy").get("id"), policyId));

        List<VirtualSystemPolicy> list = em.createQuery(query).getResultList();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list;
    }

}
