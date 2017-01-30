package org.osc.core.broker.service.persistence;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.osc.core.broker.model.entities.virtualization.openstack.HostAggregate;
import org.osc.core.broker.service.dto.openstack.HostAggregateDto;

public class HostAggregateEntityMgr {

    public static void fromEntity(HostAggregate entity, HostAggregateDto dto) {
        dto.setId(entity.getId());
        dto.setOpenstackId(entity.getOpenstackId());
    }

    public static void toEntity(HostAggregate entity, HostAggregateDto dto) {
        entity.setName(dto.getName());
    }

    @SuppressWarnings("unchecked")
    public static List<HostAggregate> listByOpenstackId(Session session, String openstackId) {
        Criteria criteria = session.createCriteria(HostAggregate.class)
                .add(Restrictions.eq("openstackId", openstackId)).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }
}
