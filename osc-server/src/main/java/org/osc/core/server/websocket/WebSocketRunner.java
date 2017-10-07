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

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.plugin.manager.WebSocketClient;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.api.ManagerApi;
import org.osc.core.broker.service.broadcast.BroadcastListener;
import org.osc.core.broker.service.broadcast.BroadcastMessage;
import org.osc.core.broker.service.broadcast.EventType;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.ApplianceManagerConnectorEntityMgr;
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

@Component(scope=ServiceScope.PROTOTYPE,
  service=WebSocketRunner.class)
public class WebSocketRunner implements BroadcastListener {
    private static final int MAX_TRIES = 10;
    private static final int TRY_WAIT_MS = 500;

    private static final Logger log = LoggerFactory.getLogger(WebSocketRunner.class);
    private final HashMap<Long, WebSocketClient> webSocketConnections = new HashMap<Long, WebSocketClient>();

    private List<ApplianceManagerConnector> amcs = new ArrayList<>();
    private ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
    private int count = MAX_TRIES;

    @Reference
    private ManagerApi managerApis;

    @Reference
    private ApiFactoryService apiFactoryService;

    @Reference
    private DBConnectionManager dbConnectionManager;

    @Reference
    private TransactionalBroadcastUtil txBroadcastUtil;

    @Reference
    private AlertGenerator alertGenerator;

    private ServiceRegistration<BroadcastListener> registration;

    @Activate
    void start(BundleContext ctx) {
        // This is not done automatically by DS as we do not want the broadcast whiteboard
        // to activate another instance of this component, only people getting the runner!
        this.registration = ctx.registerService(BroadcastListener.class, this, null);

        try {
            EntityManager em = this.dbConnectionManager.getTransactionalEntityManager();
            this.dbConnectionManager.getTransactionControl().required(() -> {
                OSCEntityManager<ApplianceManagerConnector> emgr = new OSCEntityManager<ApplianceManagerConnector>(
                        ApplianceManagerConnector.class, em, this.txBroadcastUtil);
                this.amcs.addAll(emgr.listAll());

                return null;
            });

            this.ses.schedule(new WebSocketRunnable(), TRY_WAIT_MS, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            log.error("Create DB encountered runtime exception: ", ex);
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
                        isWebSocketNotification = WebSocketRunner.this.apiFactoryService.isWebSocketNotifications(mc);
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
                    WebSocketRunner.this.alertGenerator.processSystemFailureEvent(SystemFailureType.MGR_WEB_SOCKET_NOTIFICATION_FAILURE,
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
            } else {
                WebSocketRunner.this.ses.schedule(this, TRY_WAIT_MS, TimeUnit.MILLISECONDS);
            }
        }

    }

    @Override
    public void receiveBroadcast(BroadcastMessage msg) {
        ApplianceManagerConnector mc = null;
        try {
            if (msg.getReceiver().equals("ApplianceManagerConnector")) {
                if (msg.getEventType() != EventType.DELETED) {
                    EntityManager em = this.dbConnectionManager.getTransactionalEntityManager();
                    mc = this.dbConnectionManager.getTransactionControl().required(() ->
                            ApplianceManagerConnectorEntityMgr.findById(em, msg.getEntityId()));
                } else {
                    mc = new ApplianceManagerConnector();
                    mc.setId(msg.getEntityId());
                }
                updateMCNotificationThreadMap(mc, msg.getEventType());
            }
        } catch (ScopedWorkException e) {
            log.error("Failed to create Web Socket Client for given Manager ID " + msg.getEntityId() + " due to "
                    + e.getCause().getMessage());
        } catch (Exception e) {
            log.error("Failed to create Web Socket Client for given Manager ID " + msg.getEntityId() + " due to "
                    + e.getMessage());
        }
    }

    /**
     * This method will gracefully terminate all open web socket connections
     * Used before server shutdown
     */
    @Deactivate
    void shutdown() {
        try {
            this.registration.unregister();
        } catch (IllegalStateException ise) {
            // No problem - this means the service was
            // already unregistered (e.g. by bundle stop)
        }
        closeAllConnections();
        this.webSocketConnections.clear();
    }

    private void updateMCNotificationThreadMap(ApplianceManagerConnector mc, EventType event) throws Exception {
        if (event == EventType.ADDED && this.apiFactoryService.isWebSocketNotifications(mc)) {

            /*
             * Case 1: Server started/restarted
             * This case we will add all MC type SMC in this Map and will invoke Web Socket Client for each one of them.
             * Case 2: New SMC type MC added in our database.
             * This case we will add SMC in existing Map and launch a new Web socket client for newly added MC.
             */

            this.webSocketConnections.put(mc.getId(), new WebSocketClient(mc, this.managerApis, this.apiFactoryService));

        } else if (event == EventType.UPDATED && this.apiFactoryService.isWebSocketNotifications(mc)) {

            ApplianceManagerConnector oldMC = this.webSocketConnections.get(mc.getId()).getMc();

            // Close and open a new web socket connection only if the IP/APIKey has updated of the given MC
            if (!oldMC.getIpAddress().equals(mc.getIpAddress()) || !oldMC.getApiKey().equals(mc.getApiKey())) {

                // MC is updated by the user. We close current session and remove the existing Web Socket Client from
                // the map

                closeAndRemove(mc.getId());
                // Add new one based on the new Updated MC object
                this.webSocketConnections.put(mc.getId(), new WebSocketClient(mc, this.managerApis, this.apiFactoryService));
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
