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

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;

public class SubnetEntityManager {

    public static Subnet findByOpenstackId(Session session, String id) {

        Criteria criteria = session.createCriteria(Subnet.class).add(Restrictions.eq("openstackId", id));

        return (Subnet) criteria.uniqueResult();
    }

    public static Subnet findById(Session session, Long id) {
        // Initializing Entity Manager
        EntityManager<Subnet> emgr = new EntityManager<Subnet>(Subnet.class, session);

        return emgr.findByPrimaryKey(id);
    }
}
