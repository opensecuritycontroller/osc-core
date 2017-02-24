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
