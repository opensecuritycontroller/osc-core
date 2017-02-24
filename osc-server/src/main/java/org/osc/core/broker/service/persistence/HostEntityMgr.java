package org.osc.core.broker.service.persistence;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.virtualization.openstack.Host;
import org.osc.core.broker.service.dto.openstack.HostDto;

public class HostEntityMgr {
    public static void fromEntity(Host entity, HostDto dto) {
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setOpenstackId(entity.getOpenstackId());
    }

    public static void toEntity(Host entity, HostDto dto) {
        entity.setName(dto.getName());
        entity.setOpenstackId(dto.getOpenstackId());
    }

    public static Set<HostDto> fromEntity(Set<Host> hosts) {
        Set<HostDto> hostSet = new HashSet<HostDto>();
        if (hosts != null) {
            for (Host host : hosts) {
                HostDto hsDto = new HostDto();
                HostEntityMgr.fromEntity(host, hsDto);
                hostSet.add(hsDto);
            }
        }
        return hostSet;
    }


    public static Host findById(Session session, Long id) {
        // Initializing Entity Manager
        EntityManager<Host> emgr = new EntityManager<Host>(Host.class, session);
        return emgr.findByPrimaryKey(id);
    }
}
