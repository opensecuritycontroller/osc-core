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
package org.osc.core.server.websocket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.manager.WebSocketClient;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.ApplianceManagerConnectorEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.util.BroadcastMessage;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.broker.view.util.BroadcasterUtil;
import org.osc.core.broker.view.util.BroadcasterUtil.BroadcastListener;
import org.osc.core.broker.view.util.EventType;

public class WebSocketRunner implements BroadcastListener {
    private static final int MAX_TRIES = 10;
    private static final int TRY_WAIT_MS = 500;

    private static final Logger log = Logger.getLogger(WebSocketRunner.class);
    private final HashMap<Long, WebSocketClient> webSocketConnections = new HashMap<Long, WebSocketClient>();

    private List<ApplianceManagerConnector> amcs = new ArrayList<>();
    private ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
//    private EntityManager em = null;
    private int count = MAX_TRIES;

    public WebSocketRunner() {
        BroadcasterUtil.register(this);

        EntityManager em = null;
        EntityTransaction tx = null;

        try {
            em = HibernateUtil.getEntityManagerFactory().createEntityManager();

            tx = em.getTransaction();
            tx.begin();

            OSCEntityManager<ApplianceManagerConnector> emgr = new OSCEntityManager<ApplianceManagerConnector>(
                    ApplianceManagerConnector.class, em);

            this.amcs.addAll(emgr.listAll());

            tx.commit();
            this.ses.schedule(new WebSocketRunnable(), TRY_WAIT_MS, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            log.error("Create DB encountered runtime exception: ", ex);
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        } finally {
            if (em != null) {
                em.close();
            }
        }

        /*
         * Server started/restarted will add all SMCs in this Map and will invoke Web Socket Client for each one
         * of them.
         */
    }

    private class WebSocketRunnable implements Runnable {
        @Override
        public void run() {
            Iterator<ApplianceManagerConnector> iterator = WebSocketRunner.this.amcs.iterator();

            while (iterator.hasNext()) {
                final ApplianceManagerConnector mc = iterator.next();
                boolean isWebSocketNotification = false;

                try {
                    try {
                        isWebSocketNotification = ManagerApiFactory.isWebSocketNotifications(mc);
                    } catch (VmidcException e) {
                        // VmidcException: Open Security Controller: Unsupported Manager type 'NSM'
                        String msg = e.getMessage();
                        if (msg != null && msg.contains("Unsupported Manager type")) {
                            log.warn("Plugin '" + mc.getName() + "' not yet available. count="
                                    + WebSocketRunner.this.count);
                            continue;
                        }
                        throw e;
                    }

                    iterator.remove();

                    if (isWebSocketNotification) {
                        // We start each client on a separate thread as this may block
                        updateMCNotificationThreadMap(mc, EventType.ADDED);
                    }

                } catch (Exception e) {
                    log.error("Exception during initializing web socket clients", e);
                    AlertGenerator.processSystemFailureEvent(SystemFailureType.MGR_WEB_SOCKET_NOTIFICATION_FAILURE,
                            new LockObjectReference(mc), "Failed to initialize Manager notification client for '"
                                    + mc.getName() + "' (" + e.getMessage() + ")");
                }

            }

            if (WebSocketRunner.this.amcs.isEmpty() || WebSocketRunner.this.count-- == 0) {
                if (WebSocketRunner.this.count == 0) {
                    List<String> plugins = new ArrayList<>();
                    for (ApplianceManagerConnector mc : WebSocketRunner.this.amcs) {
                        plugins.add(mc.getName());
                    }
                    log.error("Timeout waiting for the following plugins: " + plugins);
                } else {
                    log.info("Initialised websockets for all plugins");
                }
                WebSocketRunner.this.ses.shutdown();
//                WebSocketRunner.this.em.close();
            } else {
                WebSocketRunner.this.ses.schedule(this, TRY_WAIT_MS, TimeUnit.MILLISECONDS);
            }
        }

    }

    @Override
    public void receiveBroadcast(BroadcastMessage msg) {
        EntityManager em = null;
        ApplianceManagerConnector mc = null;
        try {
            if (msg.getReceiver().equals("ApplianceManagerConnector")) {
                em = HibernateUtil.getEntityManagerFactory().createEntityManager();
                if (msg.getEventType() != EventType.DELETED) {
                    mc = ApplianceManagerConnectorEntityMgr.findById(em, msg.getEntityId());
                } else {
                    mc = new ApplianceManagerConnector();
                    mc.setId(msg.getEntityId());
                }
                updateMCNotificationThreadMap(mc, msg.getEventType());
            }
        } catch (Exception e) {
            log.error("Failed to create Web Socket Client for given Manager ID " + msg.getEntityId() + " due to "
                    + e.getMessage());
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /**
     * This method will gracefully terminate all open web socket connections
     * Used before server shutdown
     */
    public void shutdown() {
        BroadcasterUtil.unregister(this);
        closeAllConnections();
        this.webSocketConnections.clear();
    }

    private void updateMCNotificationThreadMap(ApplianceManagerConnector mc, EventType event) throws Exception {
        if (event == EventType.ADDED && ManagerApiFactory.isWebSocketNotifications(mc)) {

            /*
             * Case 1: Server started/restarted
             * This case we will add all MC type SMC in this Map and will invoke Web Socket Client for each one of them.
             * Case 2: New SMC type MC added in our database.
             * This case we will add SMC in existing Map and launch a new Web socket client for newly added MC.
             */

            this.webSocketConnections.put(mc.getId(), new WebSocketClient(mc));

        } else if (event == EventType.UPDATED && ManagerApiFactory.isWebSocketNotifications(mc)) {

            ApplianceManagerConnector oldMC = this.webSocketConnections.get(mc.getId()).getMc();

            // Close and open a new web socket connection only if the IP/APIKey has updated of the given MC
            if (!oldMC.getIpAddress().equals(mc.getIpAddress()) || !oldMC.getApiKey().equals(mc.getApiKey())) {

                // MC is updated by the user. We close current session and remove the existing Web Socket Client from
                // the map

                closeAndRemove(mc.getId());
                // Add new one based on the new Updated MC object
                this.webSocketConnections.put(mc.getId(), new WebSocketClient(mc));
            }

        } else { // DELETED

            // MC is removed by the user. Closing existing web socket session with this server end point
            if (this.webSocketConnections.get(mc.getId()) != null) {
                closeAndRemove(mc.getId());
            }
        }
    }

    private void closeAndRemove(long id) {
        close(id);
        // Remove this object from map
        this.webSocketConnections.remove(id);
    }

    private void close(long id) {
        try {
            WebSocketClient client = this.webSocketConnections.get(id);
            if (client != null) {
                client.close();
            }
        } catch (Exception e) {
            log.error("Failed to close Web Socket client ", e);
        }
    }

    private void closeAllConnections() {
        try {
            Iterator<Long> iter = this.webSocketConnections.keySet().iterator();
            while (iter.hasNext()) {
                Long mcId = iter.next();
                close(mcId);
                iter.remove();
            }
        } catch (Exception ex) {
            log.error("Fail to close websocket connections", ex);
        }
    }
}
