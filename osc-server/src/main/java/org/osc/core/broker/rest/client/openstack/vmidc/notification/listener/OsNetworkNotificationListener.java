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
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationKeyType;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationObjectType;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationUtil;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.runner.RabbitMQRunner;
import org.osc.core.broker.service.DeploymentSpecConformJobFactory;
import org.osc.core.broker.service.SecurityGroupConformJobFactory;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osgi.service.transaction.control.TransactionControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsNetworkNotificationListener extends OsNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(OsNetworkNotificationListener.class);

    private final DeploymentSpecConformJobFactory dsConformJobFactory;
    private final SecurityGroupConformJobFactory sgConformJobFactory;

    private final AlertGenerator alertGenerator;

    private final TransactionControl txControl;

    public OsNetworkNotificationListener(VirtualizationConnector vc, OsNotificationObjectType objectType,
            List<String> objectIdList, BaseEntity entity, DeploymentSpecConformJobFactory dsConformJobFactory,
            SecurityGroupConformJobFactory sgConformJobFactory, AlertGenerator alertGenerator, RabbitMQRunner activeRunner,
            TransactionControl txControl) {

        super(vc, OsNotificationObjectType.NETWORK, objectIdList, entity, activeRunner);
        this.dsConformJobFactory = dsConformJobFactory;
        this.sgConformJobFactory = sgConformJobFactory;
        this.alertGenerator = alertGenerator;
        this.txControl = txControl;
        register(vc, objectType);
    }

    @Override
    public void onMessage(String message) {
        String eventType = OsNotificationUtil.getEventTypeFromMessage(message);

        // Listen to Network deleted events
        if (eventType.contains(OsNotificationEventState.DELETE.toString())) {
            String keyValue = OsNotificationUtil.isMessageRelevant(message, this.objectIdList,
                    OsNotificationKeyType.NETWORK_ID.toString());
            if (keyValue != null) {
                log.info(" [Network] : message received - " + message);
                try {
                    this.txControl.required(() -> {
                        if (this.entity instanceof SecurityGroup) {
                            this.sgConformJobFactory.startSecurityGroupConformanceJob((SecurityGroup) this.entity);
                        }

                        if (this.entity instanceof DeploymentSpec) {
                            this.dsConformJobFactory.startDsConformanceJob((DeploymentSpec) this.entity, null);
                        }
                        return null;
                    });
                } catch (Exception e) {
                    log.error("Failed post notification processing  - " + this.vc.getControllerIpAddress(), e);
                    this.alertGenerator.processSystemFailureEvent(SystemFailureType.OS_NOTIFICATION_FAILURE,
                            LockObjectReference.getLockObjectReference(this.entity, new LockObjectReference(this.vc)),
                            "Fail to process Openstack Network (" + keyValue + ") notification (" + e.getMessage()
                                    + ")");
                }
            }

        }

    }
}
