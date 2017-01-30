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
