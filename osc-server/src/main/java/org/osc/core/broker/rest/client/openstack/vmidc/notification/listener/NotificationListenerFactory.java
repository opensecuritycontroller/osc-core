package org.osc.core.broker.rest.client.openstack.vmidc.notification.listener;

import java.util.List;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationObjectType;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;

/**
 * This factory spawns new listener object and registers it based given object type to the VC specific Rabit MQ client
 */
public class NotificationListenerFactory {

    public static NotificationListener createAndRegisterNotificationListener(VirtualizationConnector vc,
            OsNotificationObjectType objectType, List<String> objectIdList, BaseEntity entity)
            throws VmidcBrokerInvalidEntryException {

        switch (objectType) {
        case PORT:
            return new OsPortNotificationListener(vc, objectType, objectIdList, entity);
        case VM:
            return new OsVMNotificationListener(vc, objectType, objectIdList, entity);
        case HOST_AGGREGRATE:
            return new OsHostAggregrateNotificationListener(vc, objectType, objectIdList, entity);
        case TENANT:
            return new OsTenantNotificationListener(vc, objectType, objectIdList, entity);
        case NETWORK:
            return new OsNetworkNotificationListener(vc, objectType, objectIdList, entity);
        default:
            break;
        }

        throw new VmidcBrokerInvalidEntryException("Invalid entry! Cannot create and register Notification for - "
                + vc.getName() + " with given inputs");
    }
}
