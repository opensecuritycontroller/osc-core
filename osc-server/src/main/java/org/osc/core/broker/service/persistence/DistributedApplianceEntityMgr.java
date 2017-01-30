package org.osc.core.broker.service.persistence;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.osc.core.util.EncryptionUtil;

public class DistributedApplianceEntityMgr {

    public static DistributedAppliance createEntity(Session session, DistributedApplianceDto dto, Appliance a,
            DistributedAppliance da) {

        toEntity(a, da, dto);

        return da;

    }

    public static void toEntity(Appliance a, DistributedAppliance da, DistributedApplianceDto dto) {

        // transform from dto to entity
        da.setId(dto.getId());
        da.setAppliance(a);
        da.setName(dto.getName());
        da.setMgrSecretKey(EncryptionUtil.encrypt(dto.getSecretKey()));
        da.setApplianceVersion(dto.getApplianceSoftwareVersionName());
    }

    public static void fromEntity(DistributedAppliance da, DistributedApplianceDto dto) {

        // transform from entity to dto
        dto.setId(da.getId());
        dto.setName(da.getName());
        dto.setApplianceManagerConnectorName(da.getApplianceManagerConnector().getName());
        dto.setMcId(da.getApplianceManagerConnector().getId());
        if (da.getLastJob() != null) {
            dto.setLastJobStatus(da.getLastJob().getStatus());
            dto.setLastJobState(da.getLastJob().getState());
            dto.setLastJobId(da.getLastJob().getId());
        }
        dto.setSecretKey(EncryptionUtil.decrypt(da.getMgrSecretKey()));
        dto.setApplianceId(da.getAppliance().getId());
        dto.setApplianceModel(da.getAppliance().getModel());
        dto.setApplianceSoftwareVersionName(da.getApplianceVersion());
        dto.setMarkForDeletion(da.getMarkedForDeletion());

        // build the Virtualization System List
        Set<VirtualSystemDto> ls = new HashSet<VirtualSystemDto>();

        for (VirtualSystem vs : da.getVirtualSystems()) {

            VirtualSystemDto vsDto = new VirtualSystemDto();
            VirtualSystemEntityMgr.fromEntity(vs, vsDto);

            ls.add(vsDto);
        }

        dto.setVirtualizationSystems(ls);
    }

    @SuppressWarnings("unchecked")
    public static List<DistributedAppliance> listAllActive(Session session) {
        Criteria criteria = session.createCriteria(DistributedAppliance.class)
                .add(Restrictions.eq("markedForDeletion", false)).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public static List<DistributedAppliance> listActiveByManagerConnector(Session session, ApplianceManagerConnector mc) {
        Criteria criteria = session.createCriteria(DistributedAppliance.class)
                .add(Restrictions.eq("markedForDeletion", false)).add(Restrictions.eq("applianceManagerConnector", mc))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }

    public static DistributedAppliance findById(Session session, Long id) {

        // Initializing Entity Manager
        EntityManager<DistributedAppliance> emgr = new EntityManager<DistributedAppliance>(DistributedAppliance.class,
                session);

        return emgr.findByPrimaryKey(id);
    }

    public static boolean isCompositeKeyExisting(Session session, String mcName, String vcName, long avID) {

        String hql = "SELECT count(*) FROM VirtualSystem VS WHERE VS.cluster.domain.applianceManagerConnector.name = :mcName AND "
                + "VS.virtualizationConnector.name = :vcName AND VS.applianceSoftwareVersion.id = :avID";

        Query query = session.createQuery(hql);
        query.setParameter("mcName", mcName);
        query.setParameter("vcName", vcName);
        query.setParameter("avID", avID);
        query.setFirstResult(0).setMaxResults(1).uniqueResult();

        Long count = (Long) query.list().get(0);

        if (count > 0) {

            return true;
        }

        return false;

    }

    public static boolean isReferencedByDistributedAppliance(Session session, ApplianceManagerConnector mc) {

        Criteria criteria = session.createCriteria(DistributedAppliance.class)
                .createAlias("applianceManagerConnector", "amc").add(Restrictions.eq("amc.id", mc.getId()));

        Long count = (Long) criteria.setProjection(Projections.rowCount()).setFirstResult(0).setMaxResults(1)
                .uniqueResult();

        if (count > 0) {

            return true;
        }

        return false;

    }

    public static boolean isReferencedByDistributedAppliance(Session session, ApplianceSoftwareVersion av) {

        String hql = "SELECT count(*) FROM DistributedAppliance DA, VirtualSystem VS WHERE VS.distributedAppliance.id = DA.id AND VS.applianceSoftwareVersion.id = :appSwVerId";
        Query query = session.createQuery(hql);
        query.setParameter("appSwVerId", av.getId());
        query.setFirstResult(0).setMaxResults(1).uniqueResult();

        Long count = (Long) query.list().get(0);

        if (count > 0) {

            return true;
        }

        return false;

    }

}
