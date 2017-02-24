package org.osc.core.broker.service.persistence;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.virtualization.openstack.AvailabilityZone;
import org.osc.core.broker.service.dto.openstack.AvailabilityZoneDto;

public class AvailabilityZoneEntityMgr {
    public static void fromEntity(AvailabilityZone entity, AvailabilityZoneDto dto) {
        dto.setId(entity.getId());
        dto.setZone(entity.getZone());
        dto.setRegion(entity.getRegion());
    }

    public static AvailabilityZone findById(Session session, Long id) {
        // Initializing Entity Manager
        EntityManager<AvailabilityZone> emgr = new EntityManager<AvailabilityZone>(AvailabilityZone.class, session);
        return emgr.findByPrimaryKey(id);
    }
}
