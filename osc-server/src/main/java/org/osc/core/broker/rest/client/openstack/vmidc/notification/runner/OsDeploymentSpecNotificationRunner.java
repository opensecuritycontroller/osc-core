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
import org.osc.core.broker.service.broadcast.BroadcastListener;
import org.osc.core.broker.service.broadcast.BroadcastMessage;
import org.osc.core.broker.service.broadcast.EventType;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.slf4j.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.transaction.control.ScopedWorkException;
import org.slf4j.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 *
 * Class will be instantiated whenever server is started and will run forever until server shutdown
 *
 */
@Component(scope=ServiceScope.PROTOTYPE,
service=OsDeploymentSpecNotificationRunner.class)
public class OsDeploymentSpecNotificationRunner implements BroadcastListener {
    @Reference
    private NotificationListenerFactory notificationListenerFactory;

    @Reference
    private TransactionalBroadcastUtil txBroadcastUtil;

    @Reference
    private DBConnectionManager dbConnectionManager;

    private final Multimap<Long, OsNotificationListener> dsToListenerMap = ArrayListMultimap.create();
    private final HashMap<Long, VirtualizationConnector> dsToVCMap = new HashMap<Long, VirtualizationConnector>();
    private ServiceRegistration<BroadcastListener> registration;

    private static final Logger log = LoggerFactory.getLogger(OsDeploymentSpecNotificationRunner.class);

    @Activate
    void start(BundleContext ctx) throws InterruptedException, VmidcException {
        // This is not done automatically by DS as we do not want the broadcast whiteboard
        // to activate another instance of this component, only people getting the runner!
        this.registration = ctx.registerService(BroadcastListener.class, this, null);
        try {
            EntityManager em = this.dbConnectionManager.getTransactionalEntityManager();
            this.dbConnectionManager.getTransactionControl().required(() -> {
                OSCEntityManager<DeploymentSpec> dsEmgr = new OSCEntityManager<DeploymentSpec>(DeploymentSpec.class,
                        em, this.txBroadcastUtil);
                List<DeploymentSpec> dsList = dsEmgr.listAll();

                for (DeploymentSpec ds : dsList) {
                    if (ds.getVirtualSystem().getVirtualizationConnector().getVirtualizationType().isOpenstack()) {
                        addListener(ds);
                    }
                }
                return null;
            });
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

    @Deactivate
    void shutdown() {
        try {
            this.registration.unregister();
        } catch (IllegalStateException ise) {
            // No problem - this means the service was
            // already unregistered (e.g. by bundle stop)
        }
        this.dsToListenerMap.clear();
        this.dsToVCMap.clear();
    }

    private void updateListenerMap(BroadcastMessage msg) {
        if (msg.getEventType() == EventType.DELETED) {
            removeListener(msg.getEntityId());
        } else {
            try {
                EntityManager em = this.dbConnectionManager.getTransactionalEntityManager();
                this.dbConnectionManager.getTransactionControl().required(() -> {
                    DeploymentSpec ds = DeploymentSpecEntityMgr.findById(em, msg.getEntityId());
                    if (ds != null && ds.getVirtualSystem().getVirtualizationConnector().getVirtualizationType().isOpenstack()) {
                        // if DS is deleted after update notification was sent
                        if (msg.getEventType() == EventType.ADDED) {
                            addListener(ds);
                        } else if (msg.getEventType() == EventType.UPDATED) {
                            for (OsNotificationListener listener : this.dsToListenerMap.get(ds.getId())) {
                                // Only Updating DS Member listener here.. DAI/SVA listener will be updated through SVA tasks
                                if (listener.getObjectType() != OsNotificationObjectType.VM) {
                                    OsNotificationUtil.updateListener(listener, ds, getMemberIdsFromDS(ds));
                                }
                            }
                        }
                    }
                    return null;
                });
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
                if (!dai.getMarkedForDeletion() && dai.getExternalId() != null) {
                    svaIdList.add(dai.getExternalId().toString());
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

            addProjectListener(ds);

            // Add DS and VC to the map. We will use this for unregistering all listeners for this DS
            this.dsToVCMap.put(ds.getId(), ds.getVirtualSystem().getVirtualizationConnector());

        } catch (VmidcBrokerInvalidEntryException e) {
            log.error("Invalid Object Type requested to register this listener with", e);
        }
    }

    private void addMemberListener(DeploymentSpec ds) throws VmidcBrokerInvalidEntryException {
        OsNotificationObjectType memberObjectType = getMemberObjectType(ds);

        // Creating member change Notification Listener
        OsNotificationListener memberListener = this.notificationListenerFactory
                .createAndRegisterNotificationListener(ds.getVirtualSystem().getVirtualizationConnector(),
                        memberObjectType, getMemberIdsFromDS(ds), ds);

        // Register Member change listener
        this.dsToListenerMap.put(ds.getId(), memberListener);

    }

    private void addProjectListener(DeploymentSpec ds) throws VmidcBrokerInvalidEntryException {
        List<String> projectIdList = new ArrayList<String>();
        projectIdList.add(ds.getProjectId());
        // Creating member change Notification Listener
        OsNotificationListener projectListener = this.notificationListenerFactory
                .createAndRegisterNotificationListener(ds.getVirtualSystem().getVirtualizationConnector(),
                        OsNotificationObjectType.PROJECT, projectIdList, ds);

        // Register Project deletion listener
        this.dsToListenerMap.put(ds.getId(), projectListener);
    }

    private void addNetworkListener(DeploymentSpec ds) throws VmidcBrokerInvalidEntryException {

        List<String> networkIdList = new ArrayList<String>();
        networkIdList.add(ds.getManagementNetworkId());

        // ADD inspection network id only if it is different from management network
        if (ds.getManagementNetworkId().equals(ds.getInspectionNetworkId())) {
            networkIdList.add(ds.getInspectionNetworkId());
        }

        // Creating member change Notification Listener
        OsNotificationListener networkListener = this.notificationListenerFactory
                .createAndRegisterNotificationListener(ds.getVirtualSystem().getVirtualizationConnector(),
                        OsNotificationObjectType.NETWORK, networkIdList, ds);

        // Register Network change listener
        this.dsToListenerMap.put(ds.getId(), networkListener);
    }

    private void addDAIListener(DeploymentSpec ds) throws VmidcBrokerInvalidEntryException {
        // Creating DAI change notification listener
        OsNotificationListener daiChangeListener = this.notificationListenerFactory
                .createAndRegisterNotificationListener(ds.getVirtualSystem().getVirtualizationConnector(),
                        OsNotificationObjectType.VM, getDAIIdsFromDS(ds), ds);
        // Register DAI change listener
        this.dsToListenerMap.put(ds.getId(), daiChangeListener);
    }

    private void removeListener(Long dsId) {
        for (OsNotificationListener listener : this.dsToListenerMap.get(dsId)) {
            listener.unRegister(this.dsToVCMap.get(dsId), listener.getObjectType());
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
    public synchronized void removeIdFromListener(Long dsId, String daiId) {
        for (OsNotificationListener listener : this.dsToListenerMap.get(dsId)) {
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
    public synchronized void addSVAIdToListener(Long dsId, String daiId) {
        for (OsNotificationListener listener : this.dsToListenerMap.get(dsId)) {
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
