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
import java.net.URI;
import java.nio.ByteBuffer;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.Session;

import org.apache.log4j.Logger;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientManager.ReconnectHandler;
import org.glassfish.tyrus.container.grizzly.client.GrizzlyClientContainer;
import org.glassfish.tyrus.container.grizzly.client.GrizzlyClientSocket;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.rest.server.api.ManagerApis;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.rest.client.crypto.SslContextProvider;
import org.osc.core.rest.client.exception.ClientResponseNotOkException;
import org.osc.core.rest.client.exception.RestClientException;
import org.osc.sdk.manager.api.ManagerWebSocketNotificationApi;

public class WebSocketClient {

    private static final int THREAD_QUEUE_LIMIT = 100;
    private static final int WEBSOCKET_SHARED_THREAD_POOL_SIZE = 10;

    private static final int DEFAULT_WEBSOCKET_RECONNECT_DELAY = 60 * 1000; // 1 minute
    private static final int DEFAULT_WEBSOCKET_IDLE_TIMEOUT = 10 * 60 * 1000; // 10 minute
    private static final int DEFAULT_WEBSOCKET_PING_DELAY = 5 * 60 * 1000; // 5 minute

    protected Session wsSession = null;
    private WebSocketClientEndPoint clientEndpoint;
    private final ApplianceManagerConnector mc;
    private final Thread initThread;
    private ManagerWebSocketNotificationApi mgrApi;
    private boolean isInterrupted = false;
    private ClientManager client;
    private CustomClientEndPointConfigurator configurator;
    private final ReconnectHandler reconnectHandler = getReconnecHandler();
    private Thread keepAliveThread;

    public ApplianceManagerConnector getMc() {
        return this.mc;
    }

    private final static Logger log = Logger.getLogger(WebSocketClient.class);
    private final ManagerApis managerApis;
    private final ApiFactoryService apiFactoryService;

    /**
     * @param mc
     *            Appliance Manager Connector (Web Socket Server) which we will be connecting to.
     * @param port
     *            Port at which Web Socket Server is listening
     * @param resourePath
     *            URL path to register/connect/subscribe
     * @param isHttps
     *            SSL enabled or disabled
     * @param webSocketClientEndpoint
     *            "ClientEndPoint" annotated object which will handle communication with server
     * @throws Exception
     *             Throws exception like {@link DeploymentException}, {@link IOException}
     *             etc..
     */
    public WebSocketClient(final ApplianceManagerConnector mc, ManagerApis managerApis, ApiFactoryService apiFactoryService) throws Exception {

        this.mc = mc;
        this.managerApis = managerApis;
        this.apiFactoryService = apiFactoryService;
        this.initThread = new Thread("WebSocketClient - " + this.mc.getName()) {
            @Override
            public void run() {
                while (!WebSocketClient.this.isInterrupted) {
                    try {
                        WebSocketClient.this.connect();
                        break;
                    } catch (InterruptedException ex) {
                        log.info("Thread: " + getName() + "is Interrupted!");
                        break;
                    } catch (Exception e) {
                        log.error(
                                "Exception during initializing web socket client for MC. "
                                        + WebSocketClient.this.mc.getName(), e);
                        AlertGenerator.processSystemFailureEvent(SystemFailureType.MGR_WEB_SOCKET_NOTIFICATION_FAILURE,
                                new LockObjectReference(mc), "Trying to initialize Manager Web Socket Api... "
                                        + WebSocketClient.this.mc.getName());
                        try {
                            sleep(60000);
                        } catch (InterruptedException ex) {
                            break;
                        }
                    }
                }
            }
        };
        this.initThread.start();
    }

    private ManagerWebSocketNotificationApi getMgrAPI() throws Exception {
        //create manager API
        return this.apiFactoryService.createManagerWebSocketNotificationApi(this.mc);
    }

    private ClientEndpointConfig getClientEndpointConfig() {
        // initializing custom client end point configurator
        this.configurator = new CustomClientEndPointConfigurator(WebSocketClient.this.mgrApi.getHandshakeParameters());
        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().configurator(this.configurator).build();

        return cec;
    }

    private void connect() throws Exception {
        this.mgrApi = getMgrAPI();
        this.clientEndpoint = new WebSocketClientEndPoint(this.mc, this.mgrApi, this.managerApis);
        this.client = ClientManager.createClient();
        if (this.mgrApi.isHttps()) {
            SSLEngineConfigurator sslEngineConfigurator = new SSLEngineConfigurator(SslContextProvider.getInstance().getSSLContext(), true,
                    false, false);
            this.client.getProperties().put(ClientManager.SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
        }

        //TODO: Future. replace idle timeout with a better way of detecting unreachable server
        this.client.setDefaultMaxSessionIdleTimeout(DEFAULT_WEBSOCKET_IDLE_TIMEOUT);
        // configuring client properties
        this.client.getProperties().put(ClientManager.RECONNECT_HANDLER, this.reconnectHandler);
        this.client.getProperties().put(GrizzlyClientContainer.SHARED_CONTAINER, true);
        // setting maximum selector thread.
        this.client.getProperties().put(
                GrizzlyClientSocket.SELECTOR_THREAD_POOL_CONFIG,
                ThreadPoolConfig.defaultConfig().setPoolName("WebSocket-Shared-Selector-Thread-Pool")
                        .setMaxPoolSize(WEBSOCKET_SHARED_THREAD_POOL_SIZE)
                        .setCorePoolSize(WEBSOCKET_SHARED_THREAD_POOL_SIZE).setQueueLimit(THREAD_QUEUE_LIMIT));

        // setting Maximum worker thread
        this.client.getProperties().put(
                GrizzlyClientSocket.WORKER_THREAD_POOL_CONFIG,
                ThreadPoolConfig.defaultConfig().setPoolName("WebSocket-Shared-Worker-Thread-Pool")
                        .setMaxPoolSize(WEBSOCKET_SHARED_THREAD_POOL_SIZE)
                        .setCorePoolSize(WEBSOCKET_SHARED_THREAD_POOL_SIZE).setQueueLimit(THREAD_QUEUE_LIMIT));

        // creating URI
        final String uri = initURI(this.mc, WebSocketClient.this.mgrApi.getPort(), this.mgrApi.getUrl(),
                WebSocketClient.this.mgrApi.isHttps());

        log.info("Connecting to web socket notifier for MC: " + URI.create(uri));
        WebSocketClient.this.wsSession = WebSocketClient.this.client.connectToServer(
                WebSocketClient.this.clientEndpoint, getClientEndpointConfig(), URI.create(uri));
        log.info("Successfully connected to web socket notifier for MC: " + URI.create(uri));

        // Launch Ping thread to keep this connection alive...
        startPinging();
    }

    /**
     * Method will allow to close opened Web Socket Connection *
     *
     * @throws IOException
     */
    public void close() throws Exception {
        log.info("Closing Web Socket Connection with - " + this.mc.getName() + "-" + this.mc.getIpAddress());
        this.isInterrupted = true;
        // check if the initThread is still running interrupt that thread
        if (this.initThread != null && !this.initThread.isInterrupted()) {
            this.initThread.interrupt();
        }

        if (this.wsSession != null && this.wsSession.isOpen()) {
            // wait until client finishes processing incoming messages then close this connection gracefully
            while (this.clientEndpoint.isProcessingMessage()) {
                try {
                    wait(500);
                } catch (Exception e) {
                    continue;
                }
            }

            WebSocketClient.this.wsSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE,
                    "Closing Connection Because MC is either Removed or Updated"));
        }

        if (this.client.getExecutorService() != null) {
            this.client.getExecutorService().shutdownNow();
        }
        if (this.client.getScheduledExecutorService() != null) {
            this.client.getScheduledExecutorService().shutdownNow();
        }

        if (this.mgrApi != null) {
            this.mgrApi.close();
        }

        // stop Ping thread...
        this.keepAliveThread.interrupt();
    }

    private String initURI(ApplianceManagerConnector mc, int port, String resourePath, boolean isHttps) {
        String uri;
        if (isHttps) {
            uri = "wss://";
        } else {
            uri = "ws://";
        }
        uri += mc.getIpAddress() + ":" + port + "/" + resourePath;
        return uri;
    }

    /**
     * @return
     *         Returns an object of Reconnect Handler
     *         Responsible for reconnecting to the server when connection loses
     *         Handle any networking issues as well
     */
    private ReconnectHandler getReconnecHandler() {
        ClientManager.ReconnectHandler reconnectHandler = new ClientManager.ReconnectHandler() {
            @Override
            public boolean onDisconnect(CloseReason closeReason) {
                // If we close the session no need to reconnect
                if (WebSocketClient.this.isInterrupted || closeReason.getCloseCode() == CloseReason.CloseCodes.NORMAL_CLOSURE) {
                    return false;
                }
                reconnect("Disconnected.... Trying to reconnect .... " + closeReason);
                return true;
            }

            @Override
            public boolean onConnectFailure(Exception exception) {
                if (!WebSocketClient.this.isInterrupted) {
                    reconnect("Connection Failure... Attempting to reconnect ..." + exception.toString());
                    return true;
                }
                return false;
            }
        };
        return reconnectHandler;
    }

    private void reconnect(String logMessage) {
        try {
            while (true) {
                try {
                    if (this.mgrApi != null) {
                        try {

                            // Attempt to logout existing session on timeout.
                            this.mgrApi.logout();
                        } catch (RestClientException e) {
                            if (e.getCause() instanceof ClientResponseNotOkException) {
                                // We get this exception when server lost the session we are trying to logout from. This happens when smc server is restarted.
                                log.error("Failed to logout session from SMC Server. Server has lost this session due to a service restart");
                            } else {
                                throw e;
                            }
                        }
                    }
                    Thread.sleep(DEFAULT_WEBSOCKET_RECONNECT_DELAY);

                    WebSocketClient.this.mgrApi = getMgrAPI();
                    WebSocketClient.this.configurator.setCookie(WebSocketClient.this.mgrApi.getHandshakeParameters());

                    log.info("Successfully got session cookie. Attempt to reconnect.");
                    break;
                } catch (Exception e) {
                    // Attempt to reconnect....
                    log.error(logMessage);

                    Thread.sleep(DEFAULT_WEBSOCKET_RECONNECT_DELAY);
                    continue;
                }
            }
        } catch (InterruptedException e) {
            log.error("Reconnect Handler is Interrupted!");
        }
    }

    public void startPinging() {
        this.keepAliveThread = new Thread("Ping Thread - " + this.mc.getName()) {
            @Override
            public void run() {
                byte[] bytes = new byte[10];
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (true) {
                    try {
                        WebSocketClient.this.wsSession = WebSocketClient.this.clientEndpoint.getActiveSession();
                        if (null != WebSocketClient.this.wsSession && WebSocketClient.this.wsSession.isOpen()) {
                            WebSocketClient.this.wsSession.getBasicRemote().sendPing(buffer);
                            log.info("Ping sent to web socket server - " + WebSocketClient.this.mc.getName());
                        }
                        Thread.sleep(DEFAULT_WEBSOCKET_PING_DELAY);
                    } catch (IOException e) {
                        log.error("Failed to ping web socket server - " + WebSocketClient.this.mc.getName());
                        continue;
                    } catch (InterruptedException e) {
                        log.info("Ping thread for web socket server - " + WebSocketClient.this.mc.getName() + " is Interupppted!");
                        break;
                    }
                }
            }
        };

        this.keepAliveThread.start();
    }
}
