package org.osc.core.broker.service.persistence;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;

public class VMEntityManager {

    public static VM findByOpenstackId(Session session, String id) {

        Criteria criteria = session.createCriteria(VM.class).add(Restrictions.eq("openstackId", id));

        return (VM) criteria.uniqueResult();
    }
}
