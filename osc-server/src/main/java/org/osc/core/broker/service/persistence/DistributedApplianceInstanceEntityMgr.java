package org.osc.core.broker.service.persistence;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;

public class DistributedApplianceInstanceEntityMgr {

    public static boolean doesDAIExist(Session session) {

        Criteria criteria = session.createCriteria(DistributedApplianceInstance.class, "dai");

        @SuppressWarnings("unchecked")
        List<DistributedApplianceInstance> list = criteria.setFirstResult(0).setMaxResults(1).list();

        if (list == null || list.size() == 0) {
            return false;
        }

        return true;
    }

    public static DistributedApplianceInstance findByNsxAgentIdAndNsxIp(Session session, String nsxAgentId,
            String nsxIpAddress) {

        Criteria criteria = session.createCriteria(DistributedApplianceInstance.class, "dai")
                .createAlias("dai.virtualSystem", "vs").createAlias("vs.virtualizationConnector", "vc")
                .add(Restrictions.eq("vc.controllerIpAddress", nsxIpAddress))
                .add(Restrictions.eq("nsxAgentId", nsxAgentId));

        @SuppressWarnings("unchecked")
        List<DistributedApplianceInstance> list = criteria.setFirstResult(0).setMaxResults(1).list();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public static DistributedApplianceInstance findByOsHostNameAndOsTenantId(Session session, String osHostName,
            String osTenantId) {

        Criteria criteria = session.createCriteria(DistributedApplianceInstance.class)
                .add(Restrictions.eq("osHostName", osHostName)).add(Restrictions.eq("osTenantId", osTenantId));

        @SuppressWarnings("unchecked")
        List<DistributedApplianceInstance> list = criteria.setFirstResult(0).setMaxResults(1).list();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    @SuppressWarnings("unchecked")
    public static List<DistributedApplianceInstance> listByVsId(Session session, Long vsId) {

        Criteria criteria = session.createCriteria(DistributedApplianceInstance.class, "dai")
                .createAlias("dai.virtualSystem", "vs").add(Restrictions.eq("vs.id", vsId))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }

    /**
     * Get DAI's by Id. If unable to find any DAI, throws VmidcBrokerValidationException.
     * @param session
     * @param daiIds
     * @return
     * @throws VmidcBrokerValidationException
     */
    public static List<DistributedApplianceInstance> getByIds(Session session, List<Long> daiIds)
            throws VmidcBrokerValidationException {
        List<DistributedApplianceInstance> daiList = new ArrayList<>();
        if (daiIds != null) {
            for (Long daiId : daiIds) {
                // fetching DAIs based upon received DAI-DTOs
                DistributedApplianceInstance dai = findById(session, daiId);
                if (dai == null) {
                    throw new VmidcBrokerValidationException(
                            "Distributed Appliance Instance with ID " + daiId + " is not found.");
                }
                daiList.add(dai);
            }
        }

        return daiList;
    }

    public static List<Long> listByMcId(Session session, Long mcId) {

        Criteria criteria = session.createCriteria(DistributedApplianceInstance.class, "dai")
                .createAlias("dai.virtualSystem", "vs").createAlias("vs.distributedAppliance", "da")
                .createAlias("da.applianceManagerConnector", "mc").add(Restrictions.eq("mc.id", mcId))
                .setProjection(Projections.property("id")).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        @SuppressWarnings("unchecked")
        List<Long> list = criteria.list();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list;
    }

    public static List<Long> listByVcId(Session session, Long vcId) {

        Criteria criteria = session.createCriteria(DistributedApplianceInstance.class, "dai")
                .createAlias("dai.virtualSystem", "vs").createAlias("vs.virtualizationConnector", "vc")
                .add(Restrictions.eq("vc.id", vcId)).setProjection(Projections.property("id"))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        @SuppressWarnings("unchecked")
        List<Long> list = criteria.list();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    public static List<String> listOsServerIdByVcId(Session session, Long vcId) {

        Criteria criteria = session.createCriteria(DistributedApplianceInstance.class, "dai")
                .createAlias("dai.virtualSystem", "vs").createAlias("vs.virtualizationConnector", "vc")
                .add(Restrictions.eq("vc.id", vcId)).setProjection(Projections.property("osServerId"))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }

    public static List<Long> listByDaId(Session session, Long daId) {

        Criteria criteria = session.createCriteria(DistributedApplianceInstance.class, "dai")
                .createAlias("dai.virtualSystem", "vs").createAlias("vs.distributedAppliance", "da")
                .add(Restrictions.eq("da.id", daId)).setProjection(Projections.property("id"))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        @SuppressWarnings("unchecked")
        List<Long> list = criteria.list();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list;
    }

    public static DistributedApplianceInstance findById(Session session, Long id) {

        // Initializing Entity Manager
        EntityManager<DistributedApplianceInstance> emgr = new EntityManager<DistributedApplianceInstance>(
                DistributedApplianceInstance.class, session);

        return emgr.findByPrimaryKey(id);
    }

    public static DistributedApplianceInstance findByName(Session session, String name) {

        EntityManager<DistributedApplianceInstance> emgr = new EntityManager<DistributedApplianceInstance>(
                DistributedApplianceInstance.class, session);

        return emgr.findByFieldName("name", name);
    }

    public static DistributedApplianceInstance findByIpAddress(Session session, String ipAddress) {

        Criteria criteria = session.createCriteria(DistributedApplianceInstance.class).add(
                Restrictions.eq("ipAddress", ipAddress));

        @SuppressWarnings("unchecked")
        List<DistributedApplianceInstance> list = criteria.setFirstResult(0).setMaxResults(1).list();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public static List<DistributedApplianceInstance> listByDsIdAndAvailabilityZone(Session session, Long dsId,
            String availabilityZone) {

        Criteria criteria = session.createCriteria(DistributedApplianceInstance.class, "dai")
                .createAlias("dai.deploymentSpec", "ds").add(Restrictions.eq("ds.id", dsId))
                .add(Restrictions.eq("osAvailabilityZone", availabilityZone))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        @SuppressWarnings("unchecked")
        List<DistributedApplianceInstance> list = criteria.list();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list;
    }

    public static List<DistributedApplianceInstance> listByDsAndHostName(Session session, DeploymentSpec ds,
            String hostName) {

        Criteria criteria = session.createCriteria(DistributedApplianceInstance.class)
                .add(Restrictions.eq("deploymentSpec", ds)).add(Restrictions.eq("osHostName", hostName))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        @SuppressWarnings("unchecked")
        List<DistributedApplianceInstance> list = criteria.list();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list;
    }

    public static boolean isReferencedByDistributedApplianceInstance(Session session, DistributedAppliance da) {

        Criteria criteria = session.createCriteria(DistributedApplianceInstance.class)
                .createAlias("virtualSystem", "vs").createAlias("vs.distributedAppliance", "da")
                .add(Restrictions.eq("da.id", da.getId()));

        Long count = (Long) criteria.setProjection(Projections.rowCount()).setFirstResult(0).setMaxResults(1)
                .uniqueResult();

        if (count > 0) {

            return true;
        }

        return false;

    }

    public static DistributedApplianceInstance getByOSServerId(Session session, String osServerId) {

        Criteria criteria = session.createCriteria(DistributedApplianceInstance.class).add(
                Restrictions.eq("osServerId", osServerId));

        @SuppressWarnings("unchecked")
        List<DistributedApplianceInstance> list = criteria.setFirstResult(0).setMaxResults(1).list();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public static DistributedApplianceInstance findByVirtualSystemAndPort(Session session, VirtualSystem vs, VMPort port) {

        Criteria criteria = session.createCriteria(DistributedApplianceInstance.class)
                .add(Restrictions.eq("virtualSystem", vs)).createAlias("protectedPorts", "ports")
                .add(Restrictions.eq("ports.id", port.getId()));

        return (DistributedApplianceInstance) criteria.uniqueResult();
    }

}
