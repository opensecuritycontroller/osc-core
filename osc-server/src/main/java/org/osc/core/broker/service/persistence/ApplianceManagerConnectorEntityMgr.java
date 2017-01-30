package org.osc.core.broker.service.persistence;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.service.dto.ApplianceManagerConnectorDto;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.util.EncryptionUtil;

public class ApplianceManagerConnectorEntityMgr {

    public static ApplianceManagerConnector createEntity(ApplianceManagerConnectorDto dto) throws Exception {
        ApplianceManagerConnector mc = new ApplianceManagerConnector();

        toEntity(mc, dto);

        return mc;
    }

    public static void toEntity(ApplianceManagerConnector mc, ApplianceManagerConnectorDto dto) throws Exception {

        // Transform from dto to entity
        mc.setId(dto.getId());
        mc.setName(dto.getName());
        mc.setManagerType(dto.getManagerType());
        mc.setServiceType(ManagerApiFactory.createApplianceManagerApi(dto.getManagerType()).getServiceName());
        mc.setIpAddress(dto.getIpAddress());
        mc.setUsername(dto.getUsername());
        mc.setPassword(EncryptionUtil.encrypt(dto.getPassword()));
        mc.setApiKey(dto.getApiKey());
        mc.setSslCertificateAttrSet(dto.getSslCertificateAttrSet());
    }

    public static void fromEntity(ApplianceManagerConnector mc, ApplianceManagerConnectorDto dto) {

        // transform from entity to dto
        dto.setId(mc.getId());
        dto.setName(mc.getName());
        dto.setManagerType(mc.getManagerType());
        dto.setIpAddress(mc.getIpAddress());
        dto.setUsername(mc.getUsername());
        dto.setPassword(EncryptionUtil.decrypt(mc.getPassword()));
        if (mc.getLastJob() != null) {
            dto.setLastJobStatus(mc.getLastJob().getStatus());
            dto.setLastJobState(mc.getLastJob().getState());
            dto.setLastJobId(mc.getLastJob().getId());
        }
        dto.setApiKey(mc.getApiKey());
        dto.setSslCertificateAttrSet(mc.getSslCertificateAttrSet());
    }

    public static ApplianceManagerConnector findById(Session session, Long id) {

        // Initializing Entity Manager
        EntityManager<ApplianceManagerConnector> emgr = new EntityManager<>(ApplianceManagerConnector.class, session);

        return emgr.findByPrimaryKey(id);
    }

    @SuppressWarnings("unchecked")
    public static List<ApplianceManagerConnector> listByManagerType(Session session, ManagerType type) {
        Criteria criteria = session.createCriteria(ApplianceManagerConnector.class)
                .add(Restrictions.eq("managerType", type.toString()))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }

    public static boolean isManagerTypeUsed(String managerType) {
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
        Transaction tx = null;
        Session session = sessionFactory.openSession();

        Long count1 = 0L;
        Long count2 = 0L;
        try {
            tx = session.beginTransaction();
            Criteria criteria1 = session.createCriteria(ApplianceManagerConnector.class).add(Restrictions.eq("managerType", managerType));
            count1 = (Long) criteria1.setProjection(Projections.rowCount()).setFirstResult(0).setMaxResults(1).uniqueResult();

            Criteria criteria2 = session.createCriteria(Appliance.class).add(Restrictions.eq("managerType", managerType));
            count2 = (Long) criteria2.setProjection(Projections.rowCount()).setFirstResult(0).setMaxResults(1).uniqueResult();

            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
        }

        if (session != null) {
            session.close();
        }

        return count1 > 0 || count2 > 0;
    }

}
