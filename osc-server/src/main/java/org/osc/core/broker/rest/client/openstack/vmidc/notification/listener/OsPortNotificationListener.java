package org.osc.core.broker.rest.client.openstack.vmidc.notification.listener;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationKeyType;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationObjectType;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationUtil;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.persistence.SubnetEntityManager;
import org.osc.core.broker.service.persistence.VMPortEntityManager;
import org.osc.core.broker.util.db.TransactionalRunner;
import org.osc.core.broker.util.db.TransactionalRunner.ErrorHandler;

public class OsPortNotificationListener extends OsNotificationListener {

    private static final Logger log = Logger.getLogger(OsPortNotificationListener.class);

    public OsPortNotificationListener(VirtualizationConnector vc, OsNotificationObjectType objectType,
            List<String> objectIdList, BaseEntity entity) {
        super(vc, OsNotificationObjectType.PORT, objectIdList, entity);
        register(vc, objectType);
    }

    @Override
    public void onMessage(final String message) {
        final String eventType = OsNotificationUtil.getEventTypeFromMessage(message);
        if (eventType.contains(OsNotificationEventState.CREATE.toString())
                || eventType.contains(OsNotificationEventState.DELETE.toString())
                || eventType.contains(OsNotificationEventState.INTERFACE_DELETE.toString())) {
            log.info(" [Port] : message received - " + message);
            if (this.entity instanceof SecurityGroup) {
                new TransactionalRunner<Void, Void>(new TransactionalRunner.ExclusiveSessionHandler())
                        .withErrorHandling(getErrorHandler()).exec(getTranscationalAction(eventType, message));
            }
        }
    }

    private TransactionalRunner.TransactionalAction<Void, Void> getTranscationalAction(final String eventType, final String message) {
        return new TransactionalRunner.TransactionalAction<Void, Void>() {
            @Override
            public Void run(Session session, Void param) throws Exception {
                if (eventType.contains(OsNotificationEventState.DELETE.toString())
                        || eventType.contains(OsNotificationEventState.INTERFACE_DELETE.toString())) {
                    handleSGPortDeletionMessages(session, message);
                } else {
                    handleSGPortMessages(session, message);
                }
                return null;
            }
        };
    }

    private ErrorHandler getErrorHandler() {
        return new ErrorHandler() {
            @Override
            public void handleError(Exception e) {
                log.error("Failed to trigger Security Group Sync on Port message Received!" + e);
                AlertGenerator.processSystemFailureEvent(SystemFailureType.OS_NOTIFICATION_FAILURE,
                        LockObjectReference.getLockObjectReference(OsPortNotificationListener.this.entity, new LockObjectReference(OsPortNotificationListener.this.vc)),
                        "Fail to process Openstack Port notification (" + e.getMessage() + ")");
            }
        };
    }

    private void handleSGPortMessages(Session session, String message) throws Exception {
        SecurityGroup sg = (SecurityGroup) this.entity;
        String keyValue;

        // load this entity from database to avoid any lazy loading issues
        sg = SecurityGroupEntityMgr.findById(session, sg.getId());

        if (sg.isProtectAll()) {
            // If protect all then check tenant id in context
            keyValue = OsNotificationUtil.isMessageRelevant(message, this.objectIdList,
                    OsNotificationKeyType.TENANT_ID.toString());

        } else {
            // check network id in context

            keyValue = OsNotificationUtil.isMessageRelevant(message, this.objectIdList,
                    OsNotificationKeyType.NETWORK_ID.toString());

            if (keyValue == null) {
                // If no match on network id then check if the register ID is of VM in context
                keyValue = OsNotificationUtil.isMessageRelevant(message, this.objectIdList,
                        OsNotificationKeyType.DEVICE_ID.toString());
            }

            if (keyValue == null) {
                keyValue = OsNotificationUtil.isMessageRelevant(message, this.objectIdList,
                        OsNotificationKeyType.SUBNET_ID.toString());

                // if key value is null by now we assume it is a subnet related notification
                if (keyValue != null) {
                    //TODO: Test the concurrency scenario when SG Sync is running and we receive a notification..

                    Subnet subnet = SubnetEntityManager.findByOpenstackId(session, keyValue);
                    String deviceOwner = OsNotificationUtil.getPropertyFromNotificationMessage(message,
                            OsNotificationKeyType.DEVICE_OWNER.toString());
                    if ((subnet.isProtectExternal() && deviceOwner.startsWith("compute:"))
                            || (!subnet.isProtectExternal() && deviceOwner.equals(""))) {
                        // we ignore message and do not trigger sync...
                        keyValue = null;
                    }
                }
            }
        }

        if (keyValue != null) {
            // start SG sync as the port is relevant to this SG object...
            triggerSGSync(sg, session);
        }

    }

    private void handleSGPortDeletionMessages(Session session, String message) throws Exception {
        SecurityGroup sg = (SecurityGroup) session.get(SecurityGroup.class, this.entity.getId());
        if (!this.objectIdList.isEmpty()) {
            if (!sg.isProtectAll()) {
                String portId = OsNotificationUtil.getPropertyFromNotificationMessage(message,
                        OsNotificationKeyType.PORT_ID.toString());
                VMPort port = VMPortEntityManager.findByOpenstackId(session, portId);
                if (port != null &&
                        ((port.getNetwork() != null && this.objectIdList.contains(port.getNetwork().getOpenstackId()))
                        || (port.getVm() != null && this.objectIdList.contains(port.getVm().getOpenstackId()))
                        || (port.getSubnet() != null && this.objectIdList.contains(port.getSubnet().getOpenstackId())))) {
                    triggerSGSync(sg, session);
                }

            } else {
                String tenantId = OsNotificationUtil.getPropertyFromNotificationMessage(message,
                        OsNotificationKeyType.CONTEXT_TENANT_ID.toString());
                if (this.objectIdList.contains(tenantId)) {
                    triggerSGSync(sg, session);
                }
            }
        }

    }

    private void triggerSGSync(SecurityGroup sg, Session session) throws Exception {
        log.info("Running SG sync based on OS Port notification received.");
        // Message is related to registered Security Group. Trigger sync
        ConformService.startSecurityGroupConformanceJob(sg);
    }
}
