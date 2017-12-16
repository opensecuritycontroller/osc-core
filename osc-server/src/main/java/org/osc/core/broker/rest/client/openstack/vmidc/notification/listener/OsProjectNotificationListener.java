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
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationKeyType;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationObjectType;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationUtil;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.runner.RabbitMQRunner;
import org.osc.core.broker.service.DeploymentSpecConformJobFactory;
import org.osc.core.broker.service.SecurityGroupConformJobFactory;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osgi.service.transaction.control.ScopedWorkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsProjectNotificationListener extends OsNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(OsProjectNotificationListener.class);

    private final DeploymentSpecConformJobFactory dsConformJobFactory;
    private final SecurityGroupConformJobFactory sgConformJobFactory;
    private final AlertGenerator alertGenerator;

    private final DBConnectionManager dbMgr;

    public OsProjectNotificationListener(VirtualizationConnector vc, OsNotificationObjectType objectType,
            List<String> objectIdList, BaseEntity entity, DeploymentSpecConformJobFactory dsConformJobFactory,
            SecurityGroupConformJobFactory sgConformJobFactory, AlertGenerator alertGenerator, RabbitMQRunner activeRunner,
            DBConnectionManager dbMgr) {
        super(vc, OsNotificationObjectType.PROJECT, objectIdList, entity, activeRunner);
        this.dsConformJobFactory = dsConformJobFactory;
        this.sgConformJobFactory = sgConformJobFactory;
        this.alertGenerator = alertGenerator;
        this.dbMgr = dbMgr;
        register(vc, objectType);
    }

    @Override
    public void onMessage(String message) {
        String eventType = OsNotificationUtil.getEventTypeFromMessage(message);
        if (eventType.contains(OsNotificationEventState.PROJECT_DELETED.toString())) {
            String keyValue = OsNotificationUtil.isMessageRelevant(message, this.objectIdList,
                    OsNotificationKeyType.RESOURCE_INFO.toString());
            if (keyValue != null) {
                log.info(" [Identity] : message received - " + message);
                try {
                    EntityManager em = this.dbMgr.getTransactionalEntityManager();

                    this.dbMgr.getTransactionControl().required(() -> {
                        if (this.entity instanceof SecurityGroup) {
                            handleSGMessages(em, keyValue);
                        } else if (this.entity instanceof DeploymentSpec) {
                            handleDSMessages(em, keyValue);
                        }
                        return null;
                    });

                } catch (ScopedWorkException e) {
                    handleException(keyValue, e.getCause());
                } catch (Exception e) {
                    handleException(keyValue, e);
                }
            }
        }
    }

    private void handleException(String keyValue, Throwable e) {
        log.error("Failed post notification processing  - " + this.vc.getControllerIpAddress(), e);
        this.alertGenerator.processSystemFailureEvent(
                        SystemFailureType.OS_NOTIFICATION_FAILURE,
                        LockObjectReference.getLockObjectReference(this.entity, new LockObjectReference(
                                this.vc)),
                        "Fail to process Openstack Project (" + keyValue + ") notification ("
                                + e.getMessage() + ")");
    }

    private void handleSGMessages(EntityManager em, String keyValue) throws Exception {
        // if Project deleted belongs to a security group
        for (SecurityGroup securityGroup : SecurityGroupEntityMgr.listByProjectId(em, keyValue)) {
            // trigger sync job for that SG
            if (securityGroup.getId().equals(((SecurityGroup) this.entity).getId())) {
                this.sgConformJobFactory.startSecurityGroupConformanceJob(securityGroup);
            }
        }
    }

    private void handleDSMessages(EntityManager em, String keyValue) throws Exception {
        // if Project deleted belongs to a deployment spec
        for (DeploymentSpec ds : DeploymentSpecEntityMgr.listDeploymentSpecByProjectId(em, keyValue)) {
            // trigger sync job for that DS
            if (ds.getId().equals(((DeploymentSpec) this.entity).getId())) {
                this.dsConformJobFactory.startDsConformanceJob(ds, null);
            }
        }
    }
}
