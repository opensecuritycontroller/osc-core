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
package org.osc.core.broker.rest.client.openstack.vmidc.notification.runner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationObjectType;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationUtil;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.listener.NotificationListenerFactory;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.listener.OsNotificationListener;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.util.BroadcastMessage;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.broker.view.util.BroadcasterUtil;
import org.osc.core.broker.view.util.BroadcasterUtil.BroadcastListener;
import org.osc.core.broker.view.util.EventType;
import org.osgi.service.transaction.control.ScopedWorkException;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 *
 * Class will be instantiated whenever server is started and will run forever until server shutdown.
 *
 */

public class OsSecurityGroupNotificationRunner implements BroadcastListener {

    private static final Multimap<Long, OsNotificationListener> sgToListenerMap = ArrayListMultimap.create();
    private static final HashMap<Long, VirtualizationConnector> sgToVCMap = new HashMap<Long, VirtualizationConnector>();

    private static final Logger log = Logger.getLogger(OsSecurityGroupNotificationRunner.class);

    public OsSecurityGroupNotificationRunner() throws InterruptedException, VmidcException {
        try {
            BroadcasterUtil.register(this);
            EntityManager em = HibernateUtil.getTransactionalEntityManager();
            HibernateUtil.getTransactionControl().required(() -> {
                OSCEntityManager<SecurityGroup> sgEmgr = new OSCEntityManager<SecurityGroup>(SecurityGroup.class, em);
                for (SecurityGroup sg : sgEmgr.listAll()) {
                    addListener(sg);
                }
                return null;
            });
        } catch (ScopedWorkException swe) {
            throw swe.asRuntimeException();
        }
    }

    @Override
    public void receiveBroadcast(BroadcastMessage msg) {
        if (msg.getReceiver().equals("SecurityGroup")) {
            updateListenerMap(msg);
        }
    }

    public void shutdown() {
        BroadcasterUtil.unregister(this);
        sgToListenerMap.clear();
        sgToVCMap.clear();
    }

    private void updateListenerMap(BroadcastMessage msg) {
        if (msg.getEventType() == EventType.DELETED) {
            removeListener(msg.getEntityId());
        } else {
            try {
                EntityManager em = HibernateUtil.getTransactionalEntityManager();
                HibernateUtil.getTransactionControl().required(() -> {
                    SecurityGroup sg = SecurityGroupEntityMgr.findById(em, msg.getEntityId());
                    if (sg == null) {
                        log.error("Processing " + msg.getEventType() + " notification for Security Group ("
                                + msg.getEntityId() + ") but couldn't find it in the DB");
                    } else if (msg.getEventType() == EventType.ADDED) {
                        addListener(sg);
                    } else if (msg.getEventType() == EventType.UPDATED) {
                        updateListeners(sg);
                    }
                    return null;
                });

            } catch (ScopedWorkException e) {
                log.error("An error occurred updating the Security Group Listeners", e.getCause());
                throw e.asRuntimeException();
            } catch (Exception e) {
                log.error("An error occurred updating the Security Group Listeners", e);
                throw new RuntimeException("Failed to consume a broadcast message", e);
            }
        }
    }

    /**
     *
     * This method will return a list of Open stack IDs based on given Security Group member type. If SGM type is null
     * then this will return a list of IDs for all SGM for the given SG
     *
     * @param sg
     *            Security Group in context
     * @param type
     *            Security Group member type
     * @return
     *         List of open stack UUIDs
     */
    private List<String> getMemberIdsFromSG(SecurityGroup sg, SecurityGroupMemberType type) {
        ArrayList<String> idList = new ArrayList<>();
        for (SecurityGroupMember sgm : sg.getSecurityGroupMembers()) {
            try {
                if (!sgm.getMarkedForDeletion() && (type == null || sgm.getType().equals(type))) {
                    idList.add(getMemberOpenstackId(sgm));
                }

            } catch (VmidcBrokerValidationException ex) {
                log.error("Failed to add SGM id to list", ex);
            }
        }
        return idList;
    }

    private String getMemberOpenstackId(SecurityGroupMember sgm) throws VmidcBrokerValidationException {
        switch (sgm.getType()) {
        case VM:
            return sgm.getVm().getOpenstackId();
        case NETWORK:
            return sgm.getNetwork().getOpenstackId();
        case SUBNET:
            return sgm.getSubnet().getOpenstackId();
        default:
            throw new VmidcBrokerValidationException("Region is not applicable for Members of type '" + sgm.getType() + "'");
        }
    }

    private void addListener(SecurityGroup sg) {
        try {

            // create VM Listener
            addMemberListener(sg, OsNotificationObjectType.VM, SecurityGroupMemberType.VM);

            // create Network Listener
            addMemberListener(sg, OsNotificationObjectType.NETWORK, SecurityGroupMemberType.NETWORK);

            // create port listener for Subnets...
            addMemberListener(sg, OsNotificationObjectType.PORT, SecurityGroupMemberType.SUBNET);

            // create Deletion Tenant Listener
            addTenantDeletionListener(sg, OsNotificationObjectType.TENANT);

            if (sg.isProtectAll()) {

                // create Port Listener with tenant Id in context
                addPortToTenantListener(sg, OsNotificationObjectType.PORT);

            } else {

                // create Port Listener with Network ID in context
                addMemberListener(sg, OsNotificationObjectType.PORT, SecurityGroupMemberType.NETWORK);

            }

            // Add new entry in SG-to-VC map
            sgToVCMap.put(sg.getId(), sg.getVirtualizationConnector());

        } catch (VmidcBrokerInvalidEntryException e) {
            log.error("Invalid Object Type requested to register this listener with", e);
        }
    }

    private void updateListeners(SecurityGroup sg) {

        for (OsNotificationListener listener : sgToListenerMap.get(sg.getId())) {

            if (listener.getObjectType().equals(OsNotificationObjectType.VM)) {

                // Updating VM listener
                OsNotificationUtil.updateListener(listener, sg, getMemberIdsFromSG(sg, SecurityGroupMemberType.VM));

            } else if (listener.getObjectType().equals(OsNotificationObjectType.NETWORK)) {

                // Updating Network listener
                OsNotificationUtil
                .updateListener(listener, sg, getMemberIdsFromSG(sg, SecurityGroupMemberType.NETWORK));

            } else if (listener.getObjectType().equals(OsNotificationObjectType.PORT)) {
                if (sg.isProtectAll()) { // type = protectALL
                    /*
                     * if SG is protectAll or is being changed by user to protectAll
                     * Update Port Listener with Tenant ID instead of Network ID(s)
                     */
                    OsNotificationUtil.updateListener(listener, sg, Arrays.asList(sg.getTenantId()));

                } else { // type = not protectALL

                    /*
                     * User changed SG from Protect All to VM/Network/Subnet.
                     * Remove tenant ID and add Network Id(s) for port listeners..
                     * or
                     * SG is not protect all and SG type is not modified... Update Member ID(s)
                     */
                    OsNotificationUtil.updateListener(listener, sg, getMemberIdsFromSG(sg, null));
                }

            }
        }
    }

    private void addPortToTenantListener(SecurityGroup sg, OsNotificationObjectType type)
            throws VmidcBrokerInvalidEntryException {
        // Creating member change Notification Listener
        OsNotificationListener listener = (OsNotificationListener) NotificationListenerFactory
                .createAndRegisterNotificationListener(sg.getVirtualizationConnector(), type,
                        Arrays.asList(sg.getTenantId()), sg);

        // Register Member change listener
        sgToListenerMap.put(sg.getId(), listener);
    }

    private void addTenantDeletionListener(SecurityGroup sg, OsNotificationObjectType type)
            throws VmidcBrokerInvalidEntryException {

        OsNotificationListener listener = (OsNotificationListener) NotificationListenerFactory
                .createAndRegisterNotificationListener(sg.getVirtualizationConnector(), type,
                        Arrays.asList(sg.getTenantId()), sg);

        // Register Member change listener
        sgToListenerMap.put(sg.getId(), listener);
    }

    private void addMemberListener(SecurityGroup sg, OsNotificationObjectType type, SecurityGroupMemberType memberType)
            throws VmidcBrokerInvalidEntryException {

        OsNotificationListener listener = null;

        if (type == OsNotificationObjectType.PORT && !sg.isProtectAll()) {
            // Create Notification Listener
            listener = (OsNotificationListener) NotificationListenerFactory.createAndRegisterNotificationListener(
                    sg.getVirtualizationConnector(), type, getMemberIdsFromSG(sg, null), sg);
        } else {
            listener = (OsNotificationListener) NotificationListenerFactory.createAndRegisterNotificationListener(
                    sg.getVirtualizationConnector(), type, getMemberIdsFromSG(sg, memberType), sg);
        }

        // Register Member change listener
        sgToListenerMap.put(sg.getId(), listener);
    }

    private void removeListener(Long sgId) {

        for (OsNotificationListener listener : sgToListenerMap.get(sgId)) {
            listener.unRegister(sgToVCMap.get(sgId), listener.getObjectType());
        }
    }
}
