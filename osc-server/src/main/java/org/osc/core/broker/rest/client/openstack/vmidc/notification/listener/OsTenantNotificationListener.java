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

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationKeyType;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationObjectType;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationUtil;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.util.db.HibernateUtil;

public class OsTenantNotificationListener extends OsNotificationListener {

    private static final Logger log = Logger.getLogger(OsTenantNotificationListener.class);

    public OsTenantNotificationListener(VirtualizationConnector vc, OsNotificationObjectType objectType,
            List<String> objectIdList, BaseEntity entity) {
        super(vc, OsNotificationObjectType.TENANT, objectIdList, entity);
        register(vc, objectType);
    }

    @Override
    public void onMessage(String message) {
        String eventType = OsNotificationUtil.getEventTypeFromMessage(message);
        if (eventType.contains(OsNotificationEventState.TENANT_DELETED.toString())) {
            String keyValue = OsNotificationUtil.isMessageRelevant(message, this.objectIdList,
                    OsNotificationKeyType.RESOURCE_INFO.toString());
            if (keyValue != null) {
                log.info(" [Identity] : message received - " + message);
                Session session = null;
                try {
                    session = HibernateUtil.getSessionFactory().openSession();
                    if (this.entity instanceof SecurityGroup) {
                        handleSGMessages(session, keyValue);
                    } else if (this.entity instanceof DeploymentSpec) {
                        handleDSMessages(session, keyValue);
                    }

                } catch (Exception e) {
                    log.error("Failed post notification processing  - " + this.vc.getControllerIpAddress(), e);
                    AlertGenerator
                            .processSystemFailureEvent(
                                    SystemFailureType.OS_NOTIFICATION_FAILURE,
                                    LockObjectReference.getLockObjectReference(this.entity, new LockObjectReference(
                                            this.vc)),
                                    "Fail to process Openstack Tenant (" + keyValue + ") notification ("
                                            + e.getMessage() + ")");
                } finally {
                    if (session != null) {
                        session.close();
                    }
                }
            }
        }
    }

    private void handleSGMessages(Session session, String keyValue) throws Exception {
        // if tenant deleted belongs to a security group
        for (SecurityGroup securityGroup : SecurityGroupEntityMgr.listByTenantId(session, keyValue)) {
            // trigger sync job for that SG
            if (securityGroup.getId().equals(((SecurityGroup) this.entity).getId())) {
                ConformService.startSecurityGroupConformanceJob(securityGroup);
            }
        }
    }

    private void handleDSMessages(Session session, String keyValue) throws Exception {
        // if tenant deleted belongs to a deployment spec
        for (DeploymentSpec ds : DeploymentSpecEntityMgr.listDeploymentSpecByTenentId(session, keyValue)) {
            // trigger sync job for that DS
            if (ds.getId().equals(((DeploymentSpec) this.entity).getId())) {
                ConformService.startDsConformanceJob(ds, null);
            }
        }
    }
}
