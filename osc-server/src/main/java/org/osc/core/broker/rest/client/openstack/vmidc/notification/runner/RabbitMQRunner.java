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

import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.virtualization.VirtualizationType;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsRabbitMQClient;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.util.BroadcastMessage;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.broker.view.util.BroadcasterUtil;
import org.osc.core.broker.view.util.BroadcasterUtil.BroadcastListener;
import org.osc.core.broker.view.util.EventType;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.encryption.EncryptionException;
import org.osgi.service.transaction.control.ScopedWorkException;

/**
 *
 * Class will be instantiated whenever server is started and will run forever until server shutdown
 *
 */
public class RabbitMQRunner implements BroadcastListener {

    private static final Logger log = Logger.getLogger(RabbitMQRunner.class);
    private static HashMap<Long, Thread> vcToRabbitMQRunnerThreadMap = new HashMap<>();
    private static HashMap<Long, OsRabbitMQClient> vcToRabbitMQClientMap = new HashMap<>();

    private OsSecurityGroupNotificationRunner securityGroupRunner;
    private OsDeploymentSpecNotificationRunner deploymentSpecRunner;

    public RabbitMQRunner() {
        try {
            BroadcasterUtil.register(this);
            EntityManager em = HibernateUtil.getTransactionalEntityManager();
            List<VirtualizationConnector> vcList =
                    HibernateUtil.getTransactionControl().required(() ->
                    VirtualizationConnectorEntityMgr.listByType(em,
                            VirtualizationType.OPENSTACK));

            // Iterate over all the VCs of Type Open Stack and start RabbitMQ connections with them.

            for (VirtualizationConnector vc : vcList) {

                /*
                 * Server start will add all VCs(Openstack) in this Map and will invoke RabbitMQ Client for each one
                 * of them.
                 */

                updateVCNotificationThreadMap(vc, EventType.ADDED);
            }

            // Initializing SG Runner here
            this.securityGroupRunner = new OsSecurityGroupNotificationRunner();

            // Initializing DS Runner here
            this.deploymentSpecRunner = new OsDeploymentSpecNotificationRunner();

        } catch (ScopedWorkException e) {
            handleError(e.getCause());
        } catch (Exception e) {
            handleError(e);
        }

    }

    private void handleError(Throwable e) {
        log.error("Exception during initializing RabbitMQ clients", e);
        AlertGenerator.processSystemFailureEvent(SystemFailureType.OS_NOTIFICATION_FAILURE,
                "Fail to initialize RabbitMQ Client (" + e.getMessage() + ")");
    }

    @Override
    public void receiveBroadcast(BroadcastMessage msg) {
        VirtualizationConnector vc = null;
        try {
            if (msg.getReceiver().equals("VirtualizationConnector")) {
                if (msg.getEventType() != EventType.DELETED) {
                    EntityManager em = HibernateUtil.getTransactionalEntityManager();
                    vc = HibernateUtil.getTransactionControl().required(() ->
                        VirtualizationConnectorEntityMgr.findById(em, msg.getEntityId()));
                } else {
                    vc = new VirtualizationConnector();
                    vc.setId(msg.getEntityId());
                }
                updateVCNotificationThreadMap(vc, msg.getEventType());
            }
        } catch (ScopedWorkException e) {
            handleError(vc, e.getCause());
        } catch (Exception e) {
            handleError(vc, e);
        }
    }

    private void handleError(VirtualizationConnector vc, Throwable e) {
        log.error("Failed to create RabbitMQ Client for given Open Stack " + vc.getId() + " due to "
                + e.getMessage());
        AlertGenerator.processSystemFailureEvent(SystemFailureType.OS_NOTIFICATION_FAILURE,
                "Fail to initialize RabbitMQ Client (" + e.getMessage() + ")");
    }

    private void updateVCNotificationThreadMap(VirtualizationConnector vc, EventType event) throws Exception {
        if (event == EventType.ADDED) {
            if (vc.getVirtualizationType() != org.osc.core.broker.model.entities.appliance.VirtualizationType.OPENSTACK) {
                return;
            }

            /*
             * Case 1: Server start
             * This case we will add all VC type Open Stack in this Map and will invoke RabbitMQ Client for each one of
             * them.
             * Case 2: New Open Stack type VC added in our database.
             * This case we will add VC in existing Map and launch a new RabbitMQ client for newly added VC.
             */

            add(vc);

        } else {

            OsRabbitMQClient client = vcToRabbitMQClientMap.get(vc.getId());
            if (client == null) {
                return;
            }

            if (event == EventType.UPDATED) {

                /*
                 * We only close and open a new Rabbit MQ connection if user Modifies
                 * RabbitMQ IP
                 * RabbitMQ User
                 * RabbitMQ Port
                 * RabbitMQ Password
                 * of the given Virtualization Connector of type OpenStack
                 */
                if (!client.getServerIP().equals(vc.getRabbitMQIP())
                        || !client.getUser().equals(
                                vc.getProviderAttributes().get(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER))
                        || !client.getPassword().equals(
                                EncryptionUtil.decryptAESCTR(vc.getProviderAttributes().get(
                                        VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD)))
                        || client.getPort() != Integer.parseInt(vc.getProviderAttributes().get(
                                VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_PORT))) {

                    closeAndDestroy(vc.getId(), false);
                    // Add new one based on the new Updated VC object
                    OsRabbitMQClient existingClient = RabbitMQRunner.vcToRabbitMQClientMap.get(vc.getId());
                    // Update attribute changes in existing client
                    existingClient.init(vc);
                    connect(existingClient, vc);
                }

            } else { // event == EventType.DELETE

                // VC is removed by the user. Closing existing RabbitMQ session with this server end point
                closeAndDestroy(vc.getId(), true);
            }
        }
    }

    /**
     *
     * This method will instantiate a new OSRabbitMQCliet instance and add it to the MAP
     *
     * @param vc
     *            VC Object for which we need a client
     */

    private void add(final VirtualizationConnector vc) throws EncryptionException {
        final OsRabbitMQClient rabbitMqClient = new OsRabbitMQClient(vc);
        // add RabbitMQCLient to Map
        vcToRabbitMQClientMap.put(vc.getId(), rabbitMqClient);
        connect(rabbitMqClient, vc);
    }

    /**
     * @param rabbitMqClient
     *            RabbitMQClient object we are trying to connect
     * @param vc
     *            VC Object for which we need a client
     */
    private void connect(final OsRabbitMQClient rabbitMqClient, final VirtualizationConnector vc) {
        // Start connection attempt in a separate thread to avoid any delays to consecutive processing
        Thread vcRabbitMQThread = new Thread("RabbitMQ Thread - " + vc.getName()) {
            @Override
            public void run() {
                try {
                    rabbitMqClient.connect();
                } catch (Exception e) {
                    log.error("Exception during initializing RabbitMQ client - " + rabbitMqClient.getServerIP(), e);
                    AlertGenerator.processSystemFailureEvent(SystemFailureType.OS_NOTIFICATION_FAILURE,
                            new LockObjectReference(vc),
                            "Fail to connect to Openstack Notification Server (" + e.getMessage() + ")");
                }
            }
        };

        vcRabbitMQThread.start();

        // Add this thread to vc/thread Map
        vcToRabbitMQRunnerThreadMap.put(vc.getId(), vcRabbitMQThread);
    }

    private void closeAndDestroy(long id, boolean destroy) {
        try {

            OsRabbitMQClient client = vcToRabbitMQClientMap.get(id);
            if (client != null) {
                client.close();
                log.info("Client closed for VcId: " + id);
                if (destroy) {
                    client.destroy();
                    log.info("Client Destroyed for VcId: " + id);
                    vcToRabbitMQClientMap.remove(id);
                }
            }

            Thread t = vcToRabbitMQRunnerThreadMap.get(id);
            if (t != null && t.isAlive()) {
                t.interrupt();
                log.info("Client Re-attempt Thread interrupted for VcId: " + id);
            }

            // Once interrupted remove this Thread from this Map
            vcToRabbitMQRunnerThreadMap.remove(id);

        } catch (Exception e) {
            log.error("Failed to close RabbitMQ client ", e);
        }
    }

    /**
    * This method will gracefully terminate all open RabbitMQ connections
    * Used before server shutdown
    */
    public void shutdown() {
        BroadcasterUtil.unregister(this);
        log.info("Unregistered RabbitMQ Runner");
        closeAllConnections();
        this.securityGroupRunner.shutdown();
        this.deploymentSpecRunner.shutdown();
    }

    private void closeAllConnections() {
        try {
            HashMap<Long, OsRabbitMQClient> rabbitMQMapCopy = new HashMap<>(vcToRabbitMQClientMap);
            for (Long vcId : rabbitMQMapCopy.keySet()) {
                closeAndDestroy(vcId, true);
            }
            vcToRabbitMQClientMap.clear();
            vcToRabbitMQRunnerThreadMap.clear();
        } catch (Exception ex) {
            log.error("Fail to close connections", ex);
        }
    }

    public static HashMap<Long, OsRabbitMQClient> getVcToRabbitMQClientMap() {
        return vcToRabbitMQClientMap;
    }
}
