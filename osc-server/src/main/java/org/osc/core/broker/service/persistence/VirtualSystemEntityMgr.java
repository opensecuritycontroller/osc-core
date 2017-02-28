/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.service.persistence;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.virtualization.VirtualizationType;
import org.osc.core.broker.service.dto.VirtualSystemDto;

public class VirtualSystemEntityMgr {

    public static void fromEntity(VirtualSystem entity, VirtualSystemDto dto) {
        dto.setId(entity.getId());
        dto.setParentId(entity.getDistributedAppliance().getId());
        dto.setName(entity.getName());
        dto.setDistributedAppliance(entity.getDistributedAppliance().getName());
        dto.setVcId(entity.getVirtualizationConnector().getId());
        dto.setVirtualizationConnectorName(entity.getVirtualizationConnector().getName());
        dto.setVirtualizationType(entity.getVirtualizationConnector().getVirtualizationType());
        dto.setMarkForDeletion(entity.getMarkedForDeletion());
        dto.setEncapsulationType(entity.getEncapsulationType());

        if (entity.getDomain() != null) {
            dto.setDomainId(entity.getDomain().getId());
            dto.setDomainName(entity.getDomain().getName());
        }
    }

    public static VirtualSystem findById(Session session, Long id) {

        // Initializing Entity Manager
        EntityManager<VirtualSystem> emgr = new EntityManager<VirtualSystem>(VirtualSystem.class, session);

        return emgr.findByPrimaryKey(id);
    }

    public static VirtualSystem findByNsxServiceInstanceIdAndVsmUuid(Session session, String serviceVsmUuid,
            String serviceInstanceId) {

        Criteria criteria = session.createCriteria(VirtualSystem.class)
                .add(Restrictions.eq("nsxVsmUuid", serviceVsmUuid))
                .add(Restrictions.eq("nsxServiceInstanceId", serviceInstanceId));

        @SuppressWarnings("unchecked")
        List<VirtualSystem> list = criteria.setFirstResult(0).setMaxResults(1).list();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public static VirtualSystem findByNsxServiceId(Session session, String nsxServiceId) {
        Criteria criteria = session.createCriteria(VirtualSystem.class)
                .add(Restrictions.eq("nsxServiceId", nsxServiceId));

        @SuppressWarnings("unchecked")
        List<VirtualSystem> list = criteria.setFirstResult(0).setMaxResults(1).list();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public static VirtualSystem findByDAAndVC(Session session, Long daId, Long vcId) {

        Criteria criteria = session.createCriteria(VirtualSystem.class).createAlias("virtualizationConnector", "vc")
                .createAlias("distributedAppliance", "da").add(Restrictions.eq("vc.id", vcId))
                .add(Restrictions.eq("da.id", daId));

        @SuppressWarnings("unchecked")
        List<VirtualSystem> list = criteria.setFirstResult(0).setMaxResults(1).list();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    @SuppressWarnings("unchecked")
    public static List<VirtualSystem> listReferencedVSBySecurityGroup(Session session, Long sgId) {

        Criteria criteria = session.createCriteria(VirtualSystem.class).createAlias("securityGroupInterfaces", "sgi")
                .createAlias("sgi.securityGroups", "sg").add(Restrictions.eq("sg.id", sgId))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        return criteria.list();
    }

    public static List<VirtualSystemDto> findByMcApplianceAndSwVer(Session session, Long mcId, Long applianceId,
            String applianceSwVer) {

        String hql = "SELECT VC.name AS vcName, VC.virtualizationType AS vcType, DO.name AS doName FROM ApplianceManagerConnector MC, Appliance A, ApplianceSoftwareVersion AV, VirtualizationConnector VC, Domain DO"
                + " WHERE A.managerType = MC.managerType AND AV.appliance.id = :applianceId AND MC.id = :mcId"
                + " AND AV.applianceSoftwareVersion = :applianceSwVer AND VC.virtualizationSoftwareVersion = AV.virtualizationSoftwareVersion"
                + " AND VC.virtualizationType = AV.virtualizationType AND DO.applianceManagerConnector.id = :mcId"
                + " ORDER BY vcName ASC";

        Query query = session.createQuery(hql);
        query.setParameter("mcId", mcId);
        query.setParameter("applianceId", applianceId);
        query.setParameter("applianceSwVer", applianceSwVer);

        @SuppressWarnings("unchecked")
        List<Object[]> ls = query.list();

        List<VirtualSystemDto> dtoList = new ArrayList<VirtualSystemDto>();

        for (Object[] arr : ls) {

            VirtualSystemDto dto = new VirtualSystemDto();

            dto.setVirtualizationConnectorName((String) arr[0]);
            dto.setVirtualizationType((VirtualizationType) arr[1]);
            dto.setDomainName((String) arr[2]);

            dtoList.add(dto);
        }

        return dtoList;

    }

    public static List<Long> listByVcId(Session session, Long vcId) {

        Criteria criteria = session.createCriteria(VirtualSystem.class, "vs")
                .createAlias("vs.virtualizationConnector", "vc").add(Restrictions.eq("vc.id", vcId))
                .setProjection(Projections.property("id")).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        @SuppressWarnings("unchecked")
        List<Long> list = criteria.list();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list;
    }

    public static VirtualSystem findByNsxServiceProfileIdAndNsxIp(Session session, String serviceProfileId,
            String nsxIpAddress) {
        Criteria criteria = session.createCriteria(VirtualSystem.class, "vs")
                .createAlias("vs.virtualizationConnector", "vc").createAlias("vs.securityGroupInterfaces", "sgi")
                .add(Restrictions.eq("sgi.tag", serviceProfileId))
                .add(Restrictions.eq("vc.controllerIpAddress", nsxIpAddress));

        return (VirtualSystem) criteria.uniqueResult();
    }

    /**
     * List all tags used within a VS and locate the next 'minimum' available tag starting with 2. If there are tags
     * 'holes' (i.e. for "1,2,3,6,7,9" - 4,5,8,10... will be available for allocation). If no 'holes' available,
     * will allocate the next minimum number (10 in our example).
     *
     * @param session
     *            database session
     * @param vs
     *            Virtual System Object to get tag for
     * @return Minimum and unique tag for given VS.
     */
    @SuppressWarnings("unchecked")
    public static synchronized Long generateUniqueTag(Session session, VirtualSystem vs) {
        vs = (VirtualSystem) session.get(VirtualSystem.class, vs.getId(), new LockOptions(LockMode.PESSIMISTIC_WRITE));
        String sql = "SELECT CONVERT(SUBSTR(tag,LOCATE('-',tag)+1), LONG) AS tag_val "
                + "FROM security_group_interface WHERE virtual_system_fk = " + vs.getId() + " ORDER BY tag_val";
        List<Object> list = session.createSQLQuery(sql).list();
        // Start with 2 as 1 is reserved in some cases
        // TODO: arvindn For Barcelona, plumgrid requires tag's larger than 300. Remove once problem is fixed on
        // plumgrid side.
        Long prevVal = 301L;
        for (Object tag : list) {
            long tagValue = ((BigInteger) tag).longValue();
            if (tagValue != prevVal) {
                return prevVal;
            }
            prevVal++;
        }
        return prevVal;
    }

}
