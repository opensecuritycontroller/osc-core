/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.sdk.sdn.element.ServiceProfileElement;

public class VirtualSystemPolicyEntityMgr {

    public static VirtualSystemPolicy findVirtualSystemPolicyByNsxId(Session session, VirtualSystem vs,
            ServiceProfileElement serviceProfile) {

        Criteria criteria = session.createCriteria(VirtualSystemPolicy.class).add(Restrictions.eq("virtualSystem", vs))
                .add(Restrictions.eq("nsxVendorTemplateId", serviceProfile.getVendorTemplateId()));

        @SuppressWarnings("unchecked")
        List<VirtualSystemPolicy> list = criteria.setFirstResult(0).setMaxResults(1).list();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public static List<VirtualSystemPolicy> listVSPolicyByPolicyId(Session session, Long policyId) {

        Criteria criteria = session.createCriteria(VirtualSystemPolicy.class).createAlias("policy", "pol")
                .add(Restrictions.eq("pol.id", policyId)).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        @SuppressWarnings("unchecked")
        List<VirtualSystemPolicy> list = criteria.list();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list;
    }

}
