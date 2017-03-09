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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.Network;
import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;

public class VMPortEntityManager {

    private static final Logger log = Logger.getLogger(VMPortEntityManager.class);

    public static VMPort findByOpenstackId(EntityManager em, String id) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<VMPort> query = cb.createQuery(VMPort.class);

        Root<VMPort> root = query.from(VMPort.class);

        query = query.select(root)
            .where(cb.equal(root.get("openstackId"), id));

        return em.createQuery(query).getSingleResult();
    }

    /**
     * Marks ports which are not valid as deleted. Basically, all ports not in the valid ports list but belonging to the
     * network are marked as deleted.
     *
     */
    public static void markStalePortsAsDeleted(EntityManager em, Network network, List<String> validPorts) {

        String hqlUpdate = "update VMPort port set port.markedForDeletion = true where port.network = :network "
                + "and port.openstackId not in :existingPortIds";

        int updatedEntities = em.createQuery(hqlUpdate).setParameter("network", network)
                .setParameter("existingPortIds", validPorts).executeUpdate();

        log.info("Marked " + updatedEntities + " Network VMPort entities as deleted");

    }

    /**
     * Marks ports which are not valid as deleted. Basically, all ports not in the valid ports list but belonging to the
     * subnet are marked as deleted.
     *
     */
    public static void markStalePortsAsDeletedForSubnet(EntityManager em, Subnet subnet, List<String> validPorts) {

        String hqlUpdate = "update VMPort port set port.markedForDeletion = true where port.subnet = :subnet "
                + "and port.openstackId not in :existingPortIds";

        int updatedEntities = em.createQuery(hqlUpdate).setParameter("subnet", subnet)
                .setParameter("existingPortIds", validPorts).executeUpdate();

        log.info("Marked " + updatedEntities + " Subnet VMPort entities as deleted");

    }

    public static VM findByIpAddress(EntityManager em, VirtualizationConnector vc, String ipAddress) {

        String hql = "SELECT VM FROM" + " VirtualizationConnector VC" + " JOIN VC.securityGroups SG"
                + " JOIN SG.securityGroupMembers SGM" + " JOIN SGM.vm VM" + " JOIN VM.ports port"
                + " JOIN port.ipAddresses ip" + " WHERE VC.id = :vcId" + " AND ip = :ipAddress";

        TypedQuery<VM> query = em.createQuery(hql, VM.class);
        query.setParameter("vcId", vc.getId());
        query.setParameter("ipAddress", ipAddress);

        List<VM> vms = query.getResultList();

        if (!vms.isEmpty()) {
            return vms.get(0);
        } else {
            return null;
        }
    }

    public static VM findByIpAddress(EntityManager em, DistributedApplianceInstance dai, String ipAddress) {

        String hql = "SELECT VM FROM" + " DistributedApplianceInstance DAI" + " JOIN DAI.protectedPorts port"
                + " JOIN port.vm VM" + " JOIN port.ipAddresses ip" + " WHERE DAI.id = :daiId" + " AND ip = :ipAddress";

        TypedQuery<VM> query = em.createQuery(hql, VM.class);
        query.setParameter("daiId", dai.getId());
        query.setParameter("ipAddress", ipAddress);

        List<VM> vms = query.getResultList();

        if (!vms.isEmpty()) {
            return vms.get(0);
        } else {
            return null;
        }
    }

    public static VM findByMacAddress(EntityManager em, String macAddress) {
        String hql = "SELECT VM FROM" + " VM VM" + " JOIN VM.ports port" + " WHERE port.macAddress = :mac";

        TypedQuery<VM> query = em.createQuery(hql, VM.class);
        query.setParameter("mac", macAddress);

        List<VM> vms = query.getResultList();

        if (!vms.isEmpty()) {
            return vms.get(0);
        } else {
            return null;
        }
    }

}
