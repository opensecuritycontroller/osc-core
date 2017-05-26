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
import org.osc.core.broker.service.broadcast.BroadcastListener;
import org.osc.core.broker.service.broadcast.BroadcastMessage;
import org.osc.core.broker.service.broadcast.EventType;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.transaction.control.ScopedWorkException;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 *
 * Class will be instantiated whenever server is started and will run forever until server shutdown.
 *
 */

@Component(scope=ServiceScope.PROTOTYPE,
 service=OsSecurityGroupNotificationRunner.class)
public class OsSecurityGroupNotificationRunner implements BroadcastListener {

    @Reference
    private NotificationListenerFactory notificationListenerFactory;

    @Reference
    private TransactionalBroadcastUtil txBroadcastUtil;

    @Reference
    private DBConnectionManager dbConnectionManager;

    private final Multimap<Long, OsNotificationListener> sgToListenerMap = ArrayListMultimap.create();
    private final HashMap<Long, VirtualizationConnector> sgToVCMap = new HashMap<Long, VirtualizationConnector>();

    private static final Logger log = Logger.getLogger(OsSecurityGroupNotificationRunner.class);
    private ServiceRegistration<BroadcastListener> registration;

    @Activate
    void start(BundleContext ctx) throws InterruptedException, VmidcException {
        // This is not done automatically by DS as we do not want the broadcast whiteboard
        // to activate another instance of this component, only people getting the runner!
        this.registration = ctx.registerService(BroadcastListener.class, this, null);

        try {
            EntityManager em = this.dbConnectionManager.getTransactionalEntityManager();
            this.dbConnectionManager.getTransactionControl().required(() -> {
                OSCEntityManager<SecurityGroup> sgEmgr = new OSCEntityManager<SecurityGroup>(SecurityGroup.class, em, this.txBroadcastUtil);
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

    @Deactivate
    void shutdown() {
        try {
            this.registration.unregister();
        } catch (IllegalStateException ise) {
            // No problem - this means the service was
            // already unregistered (e.g. by bundle stop)
        }
        this.sgToListenerMap.clear();
        this.sgToVCMap.clear();
    }

    private void updateListenerMap(BroadcastMessage msg) {
        if (msg.getEventType() == EventType.DELETED) {
            removeListener(msg.getEntityId());
        } else {
            try {
                EntityManager em = this.dbConnectionManager.getTransactionalEntityManager();
                this.dbConnectionManager.getTransactionControl().required(() -> {
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
            this.sgToVCMap.put(sg.getId(), sg.getVirtualizationConnector());

        } catch (VmidcBrokerInvalidEntryException e) {
            log.error("Invalid Object Type requested to register this listener with", e);
        }
    }

    private void updateListeners(SecurityGroup sg) {

        for (OsNotificationListener listener : this.sgToListenerMap.get(sg.getId())) {

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
        OsNotificationListener listener = this.notificationListenerFactory
                .createAndRegisterNotificationListener(sg.getVirtualizationConnector(), type,
                        Arrays.asList(sg.getTenantId()), sg);

        // Register Member change listener
        this.sgToListenerMap.put(sg.getId(), listener);
    }

    private void addTenantDeletionListener(SecurityGroup sg, OsNotificationObjectType type)
            throws VmidcBrokerInvalidEntryException {

        OsNotificationListener listener = this.notificationListenerFactory
                .createAndRegisterNotificationListener(sg.getVirtualizationConnector(), type,
                        Arrays.asList(sg.getTenantId()), sg);

        // Register Member change listener
        this.sgToListenerMap.put(sg.getId(), listener);
    }

    private void addMemberListener(SecurityGroup sg, OsNotificationObjectType type, SecurityGroupMemberType memberType)
            throws VmidcBrokerInvalidEntryException {

        OsNotificationListener listener = null;

        if (type == OsNotificationObjectType.PORT && !sg.isProtectAll()) {
            // Create Notification Listener
            listener = this.notificationListenerFactory.createAndRegisterNotificationListener(
                    sg.getVirtualizationConnector(), type, getMemberIdsFromSG(sg, null), sg);
        } else {
            listener = this.notificationListenerFactory.createAndRegisterNotificationListener(
                    sg.getVirtualizationConnector(), type, getMemberIdsFromSG(sg, memberType), sg);
        }

        // Register Member change listener
        this.sgToListenerMap.put(sg.getId(), listener);
    }

    private void removeListener(Long sgId) {

        for (OsNotificationListener listener : this.sgToListenerMap.get(sgId)) {
            listener.unRegister(this.sgToVCMap.get(sgId), listener.getObjectType());
        }
    }
}
