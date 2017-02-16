package org.osc.core.broker.rest.client.openstack.vmidc.notification;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.listener.NotificationListener;
import org.osc.core.broker.rest.server.OscAuthFilter;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.util.SessionUtil;
import org.osc.core.util.EncryptionUtil;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.osc.core.util.encryption.EncryptionException;

/**
 *
 * This class instantiates a Rabbit MQ connection with given Server and receives all the notifications which server
 * provides on the subscription
 *
 */

public class OsRabbitMQClient extends RabbitMQClient {

    private static final Logger log = Logger.getLogger(OsRabbitMQClient.class);

    private VirtualizationConnector vc;

    /*
     * A Map between ObjectType and List of Listeners (One Object type can have multiple listeners to perform different
     * operations on the same type of notifications)
     */

    private final ListMultimap<OsNotificationObjectType, NotificationListener> listenersMap = ArrayListMultimap
            .create();

    /*
     * Singleton for delegating messages to specific listeners
     */
    private final ExecutorService messageListenerService = Executors.newSingleThreadExecutor();

    public OsRabbitMQClient(VirtualizationConnector vc) throws EncryptionException {
        super();
        this.vc = vc;
        init(this.vc);
    }

    public void init(VirtualizationConnector vc) throws EncryptionException {
        this.vc = vc;
        String rabbitMQIP = this.vc.getRabbitMQIP();
        init(rabbitMQIP,
                Integer.parseInt(
                        this.vc.getProviderAttributes().get(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_PORT)),
                vc.getProviderAttributes().get(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER),
                EncryptionUtil.decryptAESCTR(
                        vc.getProviderAttributes().get(VirtualizationConnector.ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD)),
                NOVA_EXCHANGE, QUEUE_NAME, ROUTING_KEY);
    }

    @Override
    protected void init(String serverIP, int port, String user, String password, String exchange, String queue,
            String routingKey) {
        log.info("Initializing Rabbit MQ client object for server " + this.vc.getName());
        super.init(serverIP, port, user, password, exchange, queue, routingKey);
    }

    /**
     * This method adds given listener to the list of registered listeners
     *
     * @param listener
     *            Listener to be removed
     * @param objectType
     *            Event Type of the listener
     */

    public synchronized void registerListener(NotificationListener listener, OsNotificationObjectType objectType) {
        this.listenersMap.put(objectType, listener);
    }

    /**
     * This method removes given listener from the list of registered listeners
     *
     * @param listener
     *            Listener to be removed
     * @param objectType
     *            Event Type of the listener
     */

    public synchronized void removeListener(NotificationListener listener, OsNotificationObjectType objectType) {
        this.listenersMap.remove(objectType, listener);
    }

    /**
     * @param message
     *            Received Json message from Open stack Server
     * @param objectType
     *            Event Type of the incoming message
     */
    private synchronized void notifyListeners(final String message, final OsNotificationObjectType objectType) {
        if (this.listenersMap.get(objectType) != null) {
            for (final NotificationListener listener : this.listenersMap.get(objectType)) {
                this.messageListenerService.execute(new Runnable() {
                    @Override
                    public void run() {
                        listener.onMessage(message);
                    }
                });
            }
        }
    }

    public void destroy() {
        // Shutdown executor service for this instance
        this.messageListenerService.shutdown();
        // cleaning up listener map upon client close
        this.listenersMap.clear();
    }

    @Override
    protected final void receiveMessage(String message) {
        SessionUtil.setUser(OscAuthFilter.OSC_DEFAULT_LOGIN);
        log.debug(" [RabbitMQ Client Message Received ]  - " + message);
        String eventType = OsNotificationUtil.getEventTypeFromMessage(message);

        // delegate message to object specific listener
        if (!this.listenersMap.isEmpty()) {
            try {
                notifyListeners(message, OsNotificationObjectType.getType(eventType));
            } catch (IllegalArgumentException e) {
                log.error(" Disregarding notification message with unknown object type");
            }
        }
    }

    @Override
    protected void generateConnectionFailureAlert() {
        AlertGenerator.processSystemFailureEvent(SystemFailureType.OS_NOTIFICATION_FAILURE, new LockObjectReference(
                this.vc),
                "Fail to connect to Openstack Notification Server for Virtualization Connector '" + this.vc.getName()
                        + "'");
    }
}
