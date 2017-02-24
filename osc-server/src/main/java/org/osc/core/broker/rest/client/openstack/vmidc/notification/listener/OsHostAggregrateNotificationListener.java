package org.osc.core.broker.rest.client.openstack.vmidc.notification.listener;

import java.util.List;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationKeyType;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationObjectType;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationUtil;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.alert.AlertGenerator;

public class OsHostAggregrateNotificationListener extends OsNotificationListener {

    private static final Logger log = Logger.getLogger(OsHostAggregrateNotificationListener.class);

    public OsHostAggregrateNotificationListener(VirtualizationConnector vc, OsNotificationObjectType objectType,
            List<String> objectIdList, BaseEntity entity) {
        super(vc, OsNotificationObjectType.HOST_AGGREGRATE, objectIdList, entity);
        register(vc, objectType);
    }

    @Override
    public void onMessage(String message) {
        String eventType = OsNotificationUtil.getEventTypeFromMessage(message);
        if (eventType.contains(OsNotificationEventState.UPDATE_PROP.toString())
                || eventType.contains(OsNotificationEventState.ADD_HOST.toString())
                || eventType.contains(OsNotificationEventState.REMOVE_HOST.toString())) {

            String keyValue = OsNotificationUtil.isMessageRelevant(message, this.objectIdList,
                    OsNotificationKeyType.AGGREGRATE_ID.toString());
            if (keyValue != null) {
                log.info(" [Aggregrate] : message received - " + message);
                try {

                    // Trigger Sync for the related Deployment Spec
                    ConformService.startDsConformanceJob((DeploymentSpec) this.entity, null);

                } catch (Exception e) {
                    log.error("Failed post notification processing  - " + this.vc.getControllerIpAddress(), e);
                    AlertGenerator.processSystemFailureEvent(
                            SystemFailureType.OS_NOTIFICATION_FAILURE,
                            LockObjectReference.getLockObjectReference(this.entity, new LockObjectReference(this.vc)),
                            "Fail to process Openstack Host Aggregrate (" + keyValue + ") notification ("
                                    + e.getMessage() + ")");
                }
            }

        }

    }
}
