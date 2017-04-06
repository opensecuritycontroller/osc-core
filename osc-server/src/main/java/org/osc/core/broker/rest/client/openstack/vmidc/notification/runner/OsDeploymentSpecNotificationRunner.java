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
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.AvailabilityZone;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.Host;
import org.osc.core.broker.model.entities.virtualization.openstack.HostAggregate;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationObjectType;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationUtil;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.listener.NotificationListenerFactory;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.listener.OsNotificationListener;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
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
 * Class will be instantiated whenever server is started and will run forever until server shutdown
 *
 */

public class OsDeploymentSpecNotificationRunner implements BroadcastListener {
    private static final Multimap<Long, OsNotificationListener> dsToListenerMap = ArrayListMultimap.create();
    private static final HashMap<Long, VirtualizationConnector> dsToVCMap = new HashMap<Long, VirtualizationConnector>();

    private static final Logger log = Logger.getLogger(OsDeploymentSpecNotificationRunner.class);

    public OsDeploymentSpecNotificationRunner() throws InterruptedException, VmidcException {
        try {
            BroadcasterUtil.register(this);
            EntityManager em = HibernateUtil.getTransactionalEntityManager();

            List<DeploymentSpec> dsList = HibernateUtil.getTransactionControl().required(() -> {
                OSCEntityManager<DeploymentSpec> dsEmgr = new OSCEntityManager<DeploymentSpec>(DeploymentSpec.class, em);
                return dsEmgr.listAll();
            });
            for (DeploymentSpec ds : dsList) {
                addListener(ds);
            }
        } catch (ScopedWorkException ex) {
            throw ex.asRuntimeException();
        }
    }

    @Override
    public void receiveBroadcast(BroadcastMessage msg) {
        if (msg.getReceiver().equals("DeploymentSpec")) {
            updateListenerMap(msg);
        }
    }

    public void shutdown() {
        BroadcasterUtil.unregister(this);
        dsToListenerMap.clear();
        dsToVCMap.clear();
    }

    private void updateListenerMap(BroadcastMessage msg) {
        if (msg.getEventType() == EventType.DELETED) {
            removeListener(msg.getEntityId());
        } else {
            try {
                EntityManager em = HibernateUtil.getTransactionalEntityManager();
                DeploymentSpec ds = HibernateUtil.getTransactionControl().required(() ->
                         DeploymentSpecEntityMgr.findById(em, msg.getEntityId()));
                if (ds != null) {
                    // if DS is deleted after update notification was sent
                    if (msg.getEventType() == EventType.ADDED) {
                        addListener(ds);
                    } else if (msg.getEventType() == EventType.UPDATED) {
                        for (OsNotificationListener listener : dsToListenerMap.get(ds.getId())) {
                            // Only Updating DS Member listener here.. DAI/SVA listener will be updated through SVA tasks
                            if (listener.getObjectType() != OsNotificationObjectType.VM) {
                                OsNotificationUtil.updateListener(listener, ds, getMemberIdsFromDS(ds));
                            }
                        }
                    }
                }
            } catch (ScopedWorkException e) {
                log.error("An error occurred updating the Deployment Spec listeners", e.getCause());
                throw e.asRuntimeException();
            } catch (Exception e) {
                log.error("An error occurred updating the Deployment Spec listeners", e);
                throw new RuntimeException("Failed to consume a broadcast message", e);
            }
        }
    }

    private List<String> getMemberIdsFromDS(DeploymentSpec ds) {
        ArrayList<String> idList = new ArrayList<>();

        if (ds.getHostAggregates() != null) { // members are HA

            for (HostAggregate ha : ds.getHostAggregates()) {
                if (!ha.getMarkedForDeletion()) {
                    idList.add(ha.getOpenstackId());
                }
            }

        } else if (ds.getAvailabilityZones() != null) { // members are AZ

            for (AvailabilityZone az : ds.getAvailabilityZones()) {
                if (!az.getMarkedForDeletion()) {

                    // TODO: Future. Openstack. We need to decide should we piggyback on region/zone/name of the AZ
                    idList.add(az.getRegion());
                }
            }

        } else { // members are hosts in that region

            for (Host host : ds.getHosts()) {
                if (!host.getMarkedForDeletion()) {
                    idList.add(host.getOpenstackId());
                }
            }
        }
        return idList;
    }

    private List<String> getDAIIdsFromDS(DeploymentSpec ds) {
        ArrayList<String> svaIdList = new ArrayList<>();
        if (!ds.getDistributedApplianceInstances().isEmpty()) {
            for (DistributedApplianceInstance dai : ds.getDistributedApplianceInstances()) {
                if (!dai.getMarkedForDeletion() && dai.getOsServerId() != null) {
                    svaIdList.add(dai.getOsServerId().toString());
                }
            }
        }

        return svaIdList;
    }

    private void addListener(DeploymentSpec ds) {
        try {

            // Add HA listener
            addMemberListener(ds);

            // Add SVA listener
            addDAIListener(ds);

            addNetworkListener(ds);

            addTenantListener(ds);

            // Add DS and VC to the map. We will use this for unregistering all listeners for this DS
            dsToVCMap.put(ds.getId(), ds.getVirtualSystem().getVirtualizationConnector());

        } catch (VmidcBrokerInvalidEntryException e) {
            log.error("Invalid Object Type requested to register this listener with", e);
        }
    }

    private void addMemberListener(DeploymentSpec ds) throws VmidcBrokerInvalidEntryException {
        OsNotificationObjectType memberObjectType = getMemberObjectType(ds);

        // Creating member change Notification Listener
        OsNotificationListener memberListener = (OsNotificationListener) NotificationListenerFactory
                .createAndRegisterNotificationListener(ds.getVirtualSystem().getVirtualizationConnector(),
                        memberObjectType, getMemberIdsFromDS(ds), ds);

        // Register Member change listener
        dsToListenerMap.put(ds.getId(), memberListener);

    }

    private void addTenantListener(DeploymentSpec ds) throws VmidcBrokerInvalidEntryException {
        List<String> tenenatIdList = new ArrayList<String>();
        tenenatIdList.add(ds.getTenantId());
        // Creating member change Notification Listener
        OsNotificationListener tenantListener = (OsNotificationListener) NotificationListenerFactory
                .createAndRegisterNotificationListener(ds.getVirtualSystem().getVirtualizationConnector(),
                        OsNotificationObjectType.TENANT, tenenatIdList, ds);

        // Register Tenant deletion listener
        dsToListenerMap.put(ds.getId(), tenantListener);
    }

    private void addNetworkListener(DeploymentSpec ds) throws VmidcBrokerInvalidEntryException {

        List<String> networkIdList = new ArrayList<String>();
        networkIdList.add(ds.getManagementNetworkId());

        // ADD inspection network id only if it is different from management network
        if (ds.getManagementNetworkId().equals(ds.getInspectionNetworkId())) {
            networkIdList.add(ds.getInspectionNetworkId());
        }

        // Creating member change Notification Listener
        OsNotificationListener networkListener = (OsNotificationListener) NotificationListenerFactory
                .createAndRegisterNotificationListener(ds.getVirtualSystem().getVirtualizationConnector(),
                        OsNotificationObjectType.NETWORK, networkIdList, ds);

        // Register Network change listener
        dsToListenerMap.put(ds.getId(), networkListener);
    }

    private void addDAIListener(DeploymentSpec ds) throws VmidcBrokerInvalidEntryException {
        // Creating DAI change notification listener
        OsNotificationListener daiChangeListener = (OsNotificationListener) NotificationListenerFactory
                .createAndRegisterNotificationListener(ds.getVirtualSystem().getVirtualizationConnector(),
                        OsNotificationObjectType.VM, getDAIIdsFromDS(ds), ds);
        // Register DAI change listener
        dsToListenerMap.put(ds.getId(), daiChangeListener);
    }

    private void removeListener(Long dsId) {
        for (OsNotificationListener listener : dsToListenerMap.get(dsId)) {
            listener.unRegister(dsToVCMap.get(dsId), listener.getObjectType());
        }
    }

    private OsNotificationObjectType getMemberObjectType(DeploymentSpec ds) {

        // TODO: Future. Openstack. Later add support for Object Type AZ and Host as well.
        return OsNotificationObjectType.HOST_AGGREGRATE;
    }

    /**
     * @param dsId
     *            Deployment Spec ID
     * @param daiId
     *            SVA Open Stack UUID to remove from the listener list
     */
    public static synchronized void removeIdFromListener(Long dsId, String daiId) {
        for (OsNotificationListener listener : dsToListenerMap.get(dsId)) {
            if (listener.getObjectType() == OsNotificationObjectType.VM) {
                List<String> idList = listener.getObjectIdList();
                if (!idList.isEmpty() || idList.contains(daiId)) {
                    idList.remove(daiId);
                    break;
                }
            }
        }
    }

    /**
     * @param dsId
     *            Deployment Spec ID
     * @param daiId
     *            SVA Open Stack UUID to add into the listener list
     */
    public static synchronized void addSVAIdToListener(Long dsId, String daiId) {
        for (OsNotificationListener listener : dsToListenerMap.get(dsId)) {
            if (listener.getObjectType() == OsNotificationObjectType.VM) {
                List<String> idList = listener.getObjectIdList();
                if (!idList.contains(daiId)) {
                    idList.add(daiId);
                    break;
                }
            }
        }
    }
}
