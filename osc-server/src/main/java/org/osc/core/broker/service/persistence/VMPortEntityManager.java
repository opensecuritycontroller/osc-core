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

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StringType;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.Network;
import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;

public class VMPortEntityManager {

    private static final Logger log = Logger.getLogger(VMPortEntityManager.class);

    public static VMPort findByOpenstackId(Session session, String id) {
        Criteria criteria = session.createCriteria(VMPort.class).add(Restrictions.eq("openstackId", id));
        return (VMPort) criteria.uniqueResult();
    }

    /**
     * Marks ports which are not valid as deleted. Basically, all ports not in the valid ports list but belonging to the
     * network are marked as deleted.
     * 
     */
    public static void markStalePortsAsDeleted(Session session, Network network, List<String> validPorts) {

        String hqlUpdate = "update VMPort port set port.markedForDeletion = true where port.network = :network "
                + "and port.openstackId not in :existingPortIds";

        int updatedEntities = session.createQuery(hqlUpdate).setEntity("network", network)
                .setParameterList("existingPortIds", validPorts, new StringType()).executeUpdate();

        log.info("Marked " + updatedEntities + " Network VMPort entities as deleted");

    }

    /**
     * Marks ports which are not valid as deleted. Basically, all ports not in the valid ports list but belonging to the
     * subnet are marked as deleted.
     * 
     */
    public static void markStalePortsAsDeletedForSubnet(Session session, Subnet subnet, List<String> validPorts) {

        String hqlUpdate = "update VMPort port set port.markedForDeletion = true where port.subnet = :subnet "
                + "and port.openstackId not in :existingPortIds";

        int updatedEntities = session.createQuery(hqlUpdate).setEntity("subnet", subnet)
                .setParameterList("existingPortIds", validPorts, new StringType()).executeUpdate();

        log.info("Marked " + updatedEntities + " Subnet VMPort entities as deleted");

    }

    public static VM findByIpAddress(Session session, VirtualizationConnector vc, String ipAddress) {

        String hql = "SELECT VM FROM" + " VirtualizationConnector VC" + " JOIN VC.securityGroups SG"
                + " JOIN SG.securityGroupMembers SGM" + " JOIN SGM.vm VM" + " JOIN VM.ports port"
                + " JOIN port.ipAddresses ip" + " WHERE VC.id = :vcId" + " AND ip = :ipAddress";

        Query query = session.createQuery(hql);
        query.setParameter("vcId", vc.getId());
        query.setParameter("ipAddress", ipAddress);

        @SuppressWarnings("unchecked")
        List<VM> vms = query.list();

        if (!vms.isEmpty()) {
            return vms.get(0);
        } else {
            return null;
        }
    }

    public static VM findByIpAddress(Session session, DistributedApplianceInstance dai, String ipAddress) {

        String hql = "SELECT VM FROM" + " DistributedApplianceInstance DAI" + " JOIN DAI.protectedPorts port"
                + " JOIN port.vm VM" + " JOIN port.ipAddresses ip" + " WHERE DAI.id = :daiId" + " AND ip = :ipAddress";

        Query query = session.createQuery(hql);
        query.setParameter("daiId", dai.getId());
        query.setParameter("ipAddress", ipAddress);

        @SuppressWarnings("unchecked")
        List<VM> vms = query.list();

        if (!vms.isEmpty()) {
            return vms.get(0);
        } else {
            return null;
        }
    }

    public static VM findByMacAddress(Session session, String macAddress) {
        String hql = "SELECT VM FROM" + " VM VM" + " JOIN VM.ports port" + " WHERE port.macAddress = :mac";

        Query query = session.createQuery(hql);
        query.setParameter("mac", macAddress);

        @SuppressWarnings("unchecked")
        List<VM> vms = query.list();

        if (!vms.isEmpty()) {
            return vms.get(0);
        } else {
            return null;
        }
    }

}
