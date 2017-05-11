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

import java.util.List;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationObjectType;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This factory spawns new listener object and registers it based given object type to the VC specific Rabit MQ client
 */
@Component(service = NotificationListenerFactory.class)
public class NotificationListenerFactory {

    @Reference
    private ConformService conformService;

    public OsNotificationListener createAndRegisterNotificationListener(VirtualizationConnector vc,
            OsNotificationObjectType objectType, List<String> objectIdList, BaseEntity entity)
            throws VmidcBrokerInvalidEntryException {

        switch (objectType) {
        case PORT:
            return new OsPortNotificationListener(vc, objectType, objectIdList, entity);
        case VM:
            return new OsVMNotificationListener(vc, objectType, objectIdList, entity, this.conformService);
        case HOST_AGGREGRATE:
            return new OsHostAggregrateNotificationListener(vc, objectType, objectIdList, entity, this.conformService);
        case TENANT:
            return new OsTenantNotificationListener(vc, objectType, objectIdList, entity, this.conformService);
        case NETWORK:
            return new OsNetworkNotificationListener(vc, objectType, objectIdList, entity, this.conformService);
        default:
            break;
        }

        throw new VmidcBrokerInvalidEntryException("Invalid entry! Cannot create and register Notification for - "
                + vc.getName() + " with given inputs");
    }
}
