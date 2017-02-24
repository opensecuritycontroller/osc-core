package org.osc.core.broker.service.persistence;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.virtualization.VirtualizationType;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.encryption.EncryptionException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VirtualizationConnectorEntityMgr {

    public static VirtualizationConnector createEntity(// for add
            VirtualizationConnectorDto dto) throws Exception {
        VirtualizationConnector vc = new VirtualizationConnector();

        toEntity(vc, dto);

        return vc;

    }

    public static void toEntity(VirtualizationConnector vc, VirtualizationConnectorDto dto) throws EncryptionException {

        // transform from dto to entity
        vc.setId(dto.getId());
        vc.setName(dto.getName());
        vc.setVirtualizationType(dto.getType());

        vc.setControllerType(dto.getControllerType());
        if (dto.isControllerDefined()) {
            vc.setControllerIpAddress(dto.getControllerIP());
            vc.setControllerUsername(dto.getControllerUser());
            vc.setControllerPassword(EncryptionUtil.encryptAESCTR(dto.getControllerPassword()));
        } else {
            vc.setControllerIpAddress(null);
            vc.setControllerUsername(null);
            vc.setControllerPassword(null);
        }

        vc.setProviderIpAddress(dto.getProviderIP());
        vc.setProviderUsername(dto.getProviderUser());
        vc.setProviderPassword(EncryptionUtil.encryptAESCTR(dto.getProviderPassword()));
        vc.setAdminTenantName(dto.getAdminTenantName());
        vc.getProviderAttributes().putAll(dto.getProviderAttributes());
        vc.setSslCertificateAttrSet(dto.getSslCertificateAttrSet());

        vc.setVirtualizationSoftwareVersion(dto.getSoftwareVersion());
    }

    public static void fromEntity(VirtualizationConnector vc, VirtualizationConnectorDto dto) throws EncryptionException {

        // transform from entity to dto
        dto.setId(vc.getId());
        dto.setName(vc.getName());
        dto.setType(vc.getVirtualizationType());

        dto.setControllerType(vc.getControllerType());
        dto.setControllerIP(vc.getControllerIpAddress());
        dto.setControllerUser(vc.getControllerUsername());
        dto.setControllerPassword(EncryptionUtil.decryptAESCTR(vc.getControllerPassword()));

        dto.setProviderIP(vc.getProviderIpAddress());
        dto.setProviderUser(vc.getProviderUsername());
        dto.setProviderPassword(EncryptionUtil.decryptAESCTR(vc.getProviderPassword()));
        dto.setAdminTenantName(vc.getProviderAdminTenantName());
        dto.getProviderAttributes().putAll(vc.getProviderAttributes());
        dto.setSslCertificateAttrSet(vc.getSslCertificateAttrSet().stream().collect(Collectors.toSet()));

        dto.setSoftwareVersion(vc.getVirtualizationSoftwareVersion());
    }

    public static VirtualizationConnector findByName(Session session, String name) {

        // Initializing Entity Manager
        EntityManager<VirtualizationConnector> emgr = new EntityManager<VirtualizationConnector>(
                VirtualizationConnector.class, session);

        return emgr.findByFieldName("name", name);
    }

    public static List<VirtualizationConnector> listBySwVersion(Session session, String swVersion) {

        // get appliance software version based on software version provided
        EntityManager<ApplianceSoftwareVersion> emgr1 = new EntityManager<ApplianceSoftwareVersion>(
                ApplianceSoftwareVersion.class, session);
        List<ApplianceSoftwareVersion> asvList = emgr1.listByFieldName("applianceSoftwareVersion", swVersion);

        // Initializing Entity Manager
        EntityManager<VirtualizationConnector> emgr = new EntityManager<VirtualizationConnector>(
                VirtualizationConnector.class, session);

        ArrayList<VirtualizationConnector> vcList = new ArrayList<VirtualizationConnector>();

        // get all VCs based on the appliance software version
        // and virtualization software version.
        for (ApplianceSoftwareVersion avs : asvList) {
            vcList.addAll(
                    emgr.listByFieldName("virtualizationSoftwareVersion", avs.getVirtualizarionSoftwareVersion()));
        }

        return vcList;

    }

    public static void validateCanBeDeleted(Session session, VirtualizationConnector vc)
            throws VmidcBrokerInvalidRequestException {

        Criteria criteria = session.createCriteria(DistributedAppliance.class).setProjection(Projections.rowCount())
                .createCriteria("virtualSystems").add(Restrictions.eq("virtualizationConnector", vc));

        Long daCount = (Long) criteria.uniqueResult();

        if (daCount > 0) {
            throw new VmidcBrokerInvalidRequestException(
                    "Cannot delete Virtualization Connector that is referenced by a Distributed Appliance.");
        }

        criteria = session.createCriteria(SecurityGroup.class).setProjection(Projections.rowCount())
                .add(Restrictions.eq("virtualizationConnector", vc));

        Long sgCount = (Long) criteria.uniqueResult();

        if (sgCount > 0) {
            throw new VmidcBrokerInvalidRequestException(
                    "Cannot delete Virtualization Connector that is referenced by Security Groups.");
        }

    }

    public static VirtualizationConnector findById(Session session, Long id) {

        // Initializing Entity Manager
        EntityManager<VirtualizationConnector> emgr = new EntityManager<VirtualizationConnector>(
                VirtualizationConnector.class, session);

        return emgr.findByPrimaryKey(id);
    }

    @SuppressWarnings("unchecked")
    public static List<VirtualizationConnector> listByType(Session session, VirtualizationType type) {
        Criteria criteria = session.createCriteria(VirtualizationConnector.class)
                .add(Restrictions.eq("virtualizationType", type)).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }

    public static boolean isControllerTypeUsed(String controllerType) {
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
        Transaction tx = null;
        Session session = sessionFactory.openSession();
        Long count = 0L;
        try {
            tx = session.beginTransaction();
            Criteria criteria1 = session.createCriteria(VirtualizationConnector.class).add(Restrictions.eq("controllerType", controllerType));
            count = (Long) criteria1.setProjection(Projections.rowCount()).setFirstResult(0).setMaxResults(1).uniqueResult();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
        }

        if (session != null) {
            session.close();
        }

        return count > 0;
    }

}
