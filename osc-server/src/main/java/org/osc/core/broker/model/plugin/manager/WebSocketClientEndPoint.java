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
package org.osc.core.broker.model.plugin.manager;

import java.io.IOException;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.rest.server.OscAuthFilter;
import org.osc.core.broker.rest.server.api.ManagerApis;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.sdk.manager.api.ManagerWebSocketNotificationApi;
import org.osc.sdk.manager.element.MgrChangeNotification;

public class WebSocketClientEndPoint extends Endpoint {

    private static final Logger log = Logger.getLogger(WebSocketClientEndPoint.class);

    private boolean isProcessingMessage = false;
    private final ApplianceManagerConnector mc;
    private final ManagerWebSocketNotificationApi mgrApi;
    private Session activeSession = null;

    public WebSocketClientEndPoint(ApplianceManagerConnector mc, ManagerWebSocketNotificationApi mgrApi)
            throws Exception {
        super();
        this.mc = mc;
        this.mgrApi = mgrApi;
    }

    public Session getActiveSession() {
        return this.activeSession;
    }

    @Override
    public void onOpen(final Session session, EndpointConfig ec) {

        log.info("Web Socket Connection opened for MC: " + this.mc.getName() + "-" + this.mc.getIpAddress());
        try {
            this.activeSession = session;
            this.mgrApi.subscribe(session);
            session.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String text) {
                    log.info("Received web socket message : " + text);
                    try {
                        WebSocketClientEndPoint.this.isProcessingMessage = true;
                        MgrChangeNotification mgrChangeNotification = WebSocketClientEndPoint.this.mgrApi
                                .translateMessage(text);
                        if (mgrChangeNotification != null) {
                            ManagerApis.triggerMcSyncService(OscAuthFilter.OSC_DEFAULT_LOGIN,
                                    WebSocketClientEndPoint.this.mc.getIpAddress(), mgrChangeNotification);
                        }

                    } catch (Exception ex) {
                        log.error("Failed to process notification from Manager: '"
                                + WebSocketClientEndPoint.this.mc.getName() + "-"
                                + WebSocketClientEndPoint.this.mc.getIpAddress() + "'");
                        AlertGenerator.processSystemFailureEvent(SystemFailureType.MGR_WEB_SOCKET_NOTIFICATION_FAILURE,
                                new LockObjectReference(WebSocketClientEndPoint.this.mc), "Failed to process notification from Manager: '"
                                        + WebSocketClientEndPoint.this.mc.getName() + "-"
                                        + WebSocketClientEndPoint.this.mc.getIpAddress() + "'");
                    } finally {
                        WebSocketClientEndPoint.this.isProcessingMessage = false;
                    }
                }
            });

        } catch (IOException e1) {
            log.error("Failed to subscribe notification from Manager: '" + this.mc.getName() + "'");
            AlertGenerator.processSystemFailureEvent(
                    SystemFailureType.MGR_WEB_SOCKET_NOTIFICATION_FAILURE,
                    "Failed to subscribe notification from Manager: '" + this.mc.getName() + "-"
                            + this.mc.getIpAddress() + "' (" + e1.getMessage() + ")");
        }
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        this.activeSession = null;
        log.info("Web Socket session closed for MC: " + this.mc.getName() + "-" + this.mc.getIpAddress()
                + " Close Reason: " + closeReason);
    }

    @Override
    public void onError(Session session, Throwable thr) {
        this.activeSession = null;
        log.error("Error in web socket connection for MC: " + this.mc.getName() + "-" + this.mc.getIpAddress(), thr);
    }

    public boolean isProcessingMessage() {
        return this.isProcessingMessage;
    }

}
