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

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.osc.core.broker.model.entities.virtualization.openstack.Network;

public class NetworkEntityManager {

    public static Network findByOpenstackId(Session session, String id) {

        Criteria criteria = session.createCriteria(Network.class).add(Restrictions.eq("openstackId", id));

        return (Network) criteria.uniqueResult();
    }

    public static Network findById(Session session, Long id) {
        // Initializing Entity Manager
        EntityManager<Network> emgr = new EntityManager<Network>(Network.class, session);

        return emgr.findByPrimaryKey(id);
    }
}
