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

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationKeyType;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationObjectType;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationUtil;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.runner.RabbitMQRunner;
import org.osc.core.broker.service.DeploymentSpecConformJobFactory;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osgi.service.transaction.control.TransactionControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsHostAggregrateNotificationListener extends OsNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(OsHostAggregrateNotificationListener.class);

    private final DeploymentSpecConformJobFactory dsConformJobFactory;

    private final AlertGenerator alertGenerator;

    private final TransactionControl txControl;

    public OsHostAggregrateNotificationListener(VirtualizationConnector vc, OsNotificationObjectType objectType,
            List<String> objectIdList, BaseEntity entity, DeploymentSpecConformJobFactory dsConformJobFactory,
            AlertGenerator alertGenerator, RabbitMQRunner activeRuner, TransactionControl txControl) {
        super(vc, OsNotificationObjectType.HOST_AGGREGRATE, objectIdList, entity, activeRuner);
        this.dsConformJobFactory = dsConformJobFactory;
        this.alertGenerator = alertGenerator;
        this.txControl = txControl;
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
                    this.txControl.required(() -> {
                        // Trigger Sync for the related Deployment Spec
                        this.dsConformJobFactory.startDsConformanceJob((DeploymentSpec) this.entity, null);
                        return null;
                    });
                } catch (Exception e) {
                    log.error("Failed post notification processing  - " + this.vc.getControllerIpAddress(), e);
                    this.alertGenerator.processSystemFailureEvent(
                            SystemFailureType.OS_NOTIFICATION_FAILURE,
                            LockObjectReference.getLockObjectReference(this.entity, new LockObjectReference(this.vc)),
                            "Fail to process Openstack Host Aggregrate (" + keyValue + ") notification ("
                                    + e.getMessage() + ")");
                }
            }

        }

    }
}
