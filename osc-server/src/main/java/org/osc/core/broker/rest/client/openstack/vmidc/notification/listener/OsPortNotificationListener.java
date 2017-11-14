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

import javax.persistence.EntityManager;

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationKeyType;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationObjectType;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationUtil;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.runner.RabbitMQRunner;
import org.osc.core.broker.service.SecurityGroupConformJobFactory;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.persistence.VMPortEntityManager;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osgi.service.transaction.control.ScopedWorkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsPortNotificationListener extends OsNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(OsPortNotificationListener.class);

    private final SecurityGroupConformJobFactory sgConformJobFactory;

    private final AlertGenerator alertGenerator;

    private final DBConnectionManager dbMgr;

    public OsPortNotificationListener(VirtualizationConnector vc, OsNotificationObjectType objectType,
            List<String> objectIdList, BaseEntity entity, SecurityGroupConformJobFactory sgConformJobFactory,
            AlertGenerator alertGenerator, RabbitMQRunner activeRunner, DBConnectionManager dbMgr) {
        super(vc, OsNotificationObjectType.PORT, objectIdList, entity, activeRunner);
        this.sgConformJobFactory = sgConformJobFactory;
        this.alertGenerator = alertGenerator;
        this.dbMgr = dbMgr;
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

                try {
                    doTranscationalAction(eventType, message);
                } catch (ScopedWorkException e) {
                    handleError(e.getCause());
                } catch (Exception e) {
                    handleError(e);
                }
            }
        }
    }

    private void doTranscationalAction(final String eventType, final String message) throws Exception {
        EntityManager em = this.dbMgr.getTransactionalEntityManager();
        this.dbMgr.getTransactionControl().required(() -> {
            if (eventType.contains(OsNotificationEventState.DELETE.toString())
                    || eventType.contains(OsNotificationEventState.INTERFACE_DELETE.toString())) {
                handleSGPortDeletionMessages(em, message);
            } else {
                handleSGPortMessages(em, message);
            }
            return null;
        });
    }

    private void handleError(Throwable e) {
        log.error("Failed to trigger Security Group Sync on Port message Received!" + e);
        this.alertGenerator.processSystemFailureEvent(SystemFailureType.OS_NOTIFICATION_FAILURE,
                LockObjectReference.getLockObjectReference(OsPortNotificationListener.this.entity, new LockObjectReference(OsPortNotificationListener.this.vc)),
                "Fail to process Openstack Port notification (" + e.getMessage() + ")");
    }

    private void handleSGPortMessages(EntityManager em, String message) throws Exception {
        SecurityGroup sg = (SecurityGroup) this.entity;
        String keyValue;

        // load this entity from database to avoid any lazy loading issues
        sg = SecurityGroupEntityMgr.findById(em, sg.getId());

        if (sg.isProtectAll()) {
            // If protect all then check Project id in context
            keyValue = OsNotificationUtil.isMessageRelevant(message, this.objectIdList,
                    OsNotificationKeyType.PROJECT_ID.toString());

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
                // Add subnet notification snippet back when the OpenStack device_owner issue is fixed.
                // Related OSC issue: https://github.com/opensecuritycontroller/osc-core/issues/462
            }
        }

        if (keyValue != null) {
            // start SG sync as the port is relevant to this SG object...
            triggerSGSync(sg, em);
        }

    }

    private void handleSGPortDeletionMessages(EntityManager em, String message) throws Exception {
        SecurityGroup sg = em.find(SecurityGroup.class, this.entity.getId());
        if (!this.objectIdList.isEmpty()) {
            if (!sg.isProtectAll()) {
                String portId = OsNotificationUtil.getPropertyFromNotificationMessage(message,
                        OsNotificationKeyType.PORT_ID.toString());
                VMPort port = VMPortEntityManager.findByOpenstackId(em, portId);
                if (port != null &&
                        ((port.getNetwork() != null && this.objectIdList.contains(port.getNetwork().getOpenstackId()))
                                || (port.getVm() != null && this.objectIdList.contains(port.getVm().getOpenstackId()))
                                || (port.getSubnet() != null && this.objectIdList.contains(port.getSubnet().getOpenstackId())))) {
                    triggerSGSync(sg, em);
                }

            } else {
                String projectId = OsNotificationUtil.getPropertyFromNotificationMessage(message,
                        OsNotificationKeyType.CONTEXT_PROJECT_ID.toString());
                if (this.objectIdList.contains(projectId)) {
                    triggerSGSync(sg, em);
                }
            }
        }

    }

    private void triggerSGSync(SecurityGroup sg, EntityManager em) throws Exception {
        log.info("Running SG sync based on OS Port notification received.");
        // Message is related to registered Security Group. Trigger sync
        this.sgConformJobFactory.startSecurityGroupConformanceJob(sg);
    }
}
