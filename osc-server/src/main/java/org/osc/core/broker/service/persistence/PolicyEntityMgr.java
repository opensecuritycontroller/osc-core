package org.osc.core.broker.service.persistence;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.service.policy.PolicyDto;

public class PolicyEntityMgr {

    public static void fromEntity(Policy entity, PolicyDto dto) {
        dto.setId(entity.getId());
        dto.setPolicyName(entity.getName());
        dto.setMgrPolicyId(entity.getMgrPolicyId());
        dto.setMgrDomainId(entity.getDomain().getId());
        dto.setMgrDomainName(entity.getDomain().getName());
    }

    public static Policy findById(Session session, Long id) {

        // Initializing Entity Manager
        EntityManager<Policy> emgr = new EntityManager<Policy>(
                Policy.class, session);

        return emgr.findByPrimaryKey(id);
    }

}
