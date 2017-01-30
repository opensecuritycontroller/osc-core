package org.osc.core.broker.rest.client.openstack.vmidc.notification.listener;

import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationObjectType;

public interface NotificationListener {

    /**
     *
     * This method is used to register a listener to a specific VC. Once registered TO the rabbitMQ client of that VC
     * this listener will receive messages based on the Object Type it was registered for
     *
     * @param vc
     *            Virtualization Connector you want to receive Notifications from
     * @param objectType
     *            Object Type you are interested to receive notification on
     */
    void register(VirtualizationConnector vc, OsNotificationObjectType objectType);

    /**
     *
     *
     * This method is used to un_register a listener from a specific VC's Rabbit MQ client. We unregister a listener in
     * two cases
     *
     * 1. We do not want to receive notifications for this object Type on that listener
     * 2. When we shutdown server
     *
     * @param vc
     *
     *            Virtualization Connector you want to receive Notifications from
     *
     * @param objectType
     *            Object Type you are interested to receive notification on
     */
    void unRegister(VirtualizationConnector vc, OsNotificationObjectType objectType);

    /**
     *
     * Listener will be receiving messages of interest via this method. User will Post Notification add Business Logic
     * here
     *
     * @param message
     */
    void onMessage(String message);
}
