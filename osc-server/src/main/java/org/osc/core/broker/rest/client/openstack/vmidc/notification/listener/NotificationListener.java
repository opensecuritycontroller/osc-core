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
