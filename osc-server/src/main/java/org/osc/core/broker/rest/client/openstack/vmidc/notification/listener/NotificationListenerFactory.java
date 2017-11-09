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
import java.util.concurrent.atomic.AtomicBoolean;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationObjectType;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.runner.RabbitMQRunner;
import org.osc.core.broker.service.DeploymentSpecConformJobFactory;
import org.osc.core.broker.service.SecurityGroupConformJobFactory;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This factory spawns new listener object and registers it based given object type to the VC specific Rabit MQ client
 */
@Component(service = NotificationListenerFactory.class)
public class NotificationListenerFactory {

    @Reference
    private DeploymentSpecConformJobFactory dsConformJobFactory;

    @Reference
    private SecurityGroupConformJobFactory sgConformJobFactory;

    @Reference
    private AlertGenerator alertGenerator;

    @Reference
    private DBConnectionManager dbMgr;

    // target ensures this only binds to active runner published by Server
    @Reference(target = "(active=true)", cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ComponentServiceObjects<RabbitMQRunner> activeRunnerCSO;
    private RabbitMQRunner activeRunner;

    private final AtomicBoolean initDone = new AtomicBoolean();

    @Deactivate
    private void deactivate() {
        if (this.initDone.get()) {
            this.activeRunnerCSO.ungetService(this.activeRunner);
        }
    }

    public OsNotificationListener createAndRegisterNotificationListener(VirtualizationConnector vc,
            OsNotificationObjectType objectType, List<String> objectIdList, BaseEntity entity)
            throws VmidcBrokerInvalidEntryException {

        if (this.initDone.compareAndSet(false, true)) {
            this.activeRunner = this.activeRunnerCSO.getService();
        }

        switch (objectType) {
        case PORT:
            return new OsPortNotificationListener(vc, objectType, objectIdList, entity, this.sgConformJobFactory,
                    this.alertGenerator, this.activeRunner, this.dbMgr);
        case VM:
            return new OsVMNotificationListener(vc, objectType, objectIdList, entity, this.dsConformJobFactory,
                    this.sgConformJobFactory, this.alertGenerator, this.activeRunner, this.dbMgr);
        case HOST_AGGREGRATE:
            return new OsHostAggregrateNotificationListener(vc, objectType, objectIdList, entity, this.dsConformJobFactory,
                    this.alertGenerator, this.activeRunner, this.dbMgr.getTransactionControl());
        case PROJECT:
            return new OsProjectNotificationListener(vc, objectType, objectIdList, entity, this.dsConformJobFactory,
                    this.sgConformJobFactory, this.alertGenerator, this.activeRunner, this.dbMgr);
        case NETWORK:
            return new OsNetworkNotificationListener(vc, objectType, objectIdList, entity, this.dsConformJobFactory,
                    this.sgConformJobFactory, this.alertGenerator, this.activeRunner, this.dbMgr.getTransactionControl());
        default:
            break;
        }

        throw new VmidcBrokerInvalidEntryException("Invalid entry! Cannot create and register Notification for - "
                + vc.getName() + " with given inputs");
    }
}
