package org.osc.core.broker.service.persistence;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.service.dto.ApplianceDto;



public class ApplianceEntityMgr {

    public static Appliance createEntity(// for add
            ApplianceDto dto) {
        Appliance a = new Appliance();

        toEntity(a, dto);

        return a;

    }

    public static void toEntity(Appliance a, ApplianceDto dto) {

        // transform from dto to entity
        a.setId(dto.getId());
        a.setModel(dto.getModel());
        a.setManagerType(dto.getManagerType());
        a.setManagerSoftwareVersion(dto.getManagerVersion());
    }

    public static void fromEntity(Appliance a, ApplianceDto dto) {

        // transform from entity to dto
        dto.setId(a.getId());
        dto.setModel(a.getModel());
        dto.setManagerType(ManagerType.fromText(a.getManagerType().toString()));
        dto.setManagerVersion(a.getManagerSoftwareVersion().toString());
    }

    public static Appliance findById(Session session, Long id) {

        // Initializing Entity Manager
        EntityManager<Appliance> emgr = new EntityManager<Appliance>(Appliance.class, session);

        return emgr.findByPrimaryKey(id);
    }

    public static Appliance findByModel(Session session, String model) {
        return (Appliance) session.createCriteria(Appliance.class).add(Restrictions.eq("model", model)).uniqueResult();
    }
}
