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

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.rest.client.openstack.discovery.VmDiscoveryCache;
import org.osc.core.broker.rest.client.openstack.discovery.VmDiscoveryCache.VmInfo;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationKeyType;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationObjectType;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.OsNotificationUtil;
import org.osc.core.broker.rest.server.OscAuthFilter;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.persistence.VMEntityManager;
import org.osc.core.broker.util.SessionUtil;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osgi.service.transaction.control.ScopedWorkException;

public class OsVMNotificationListener extends OsNotificationListener {

    private static final Logger log = Logger.getLogger(OsVMNotificationListener.class);
    private static final String REGION_NOTIFICATION_KEY = "region";

    public OsVMNotificationListener(VirtualizationConnector vc, OsNotificationObjectType objectType,
            List<String> objectIdList, BaseEntity entity) {
        super(vc, OsNotificationObjectType.VM, objectIdList, entity);
        register(vc, objectType);
    }

    @Override
    public void onMessage(String message) {

        String eventType = OsNotificationUtil.getEventTypeFromMessage(message);
        if (eventType.contains(OsNotificationEventState.CREATE.toString())
                || eventType.contains(OsNotificationEventState.DELETE.toString())
                || eventType.contains(OsNotificationEventState.POWER_OFF.toString())
                || eventType.contains(OsNotificationEventState.RESIZE_CONFIRM_END.toString())) {

            String vmOpenstackId = OsNotificationUtil.isMessageRelevant(message, this.objectIdList,
                    OsNotificationKeyType.INSTANCE_ID.toString());
            if (vmOpenstackId != null) {
                SessionUtil.setUser(OscAuthFilter.OSC_DEFAULT_LOGIN);

                log.info(" [Instance] : message received - " + message);
                try {
                    if (this.entity instanceof SecurityGroup) {

                        if (!eventType.contains(OsNotificationEventState.POWER_OFF.toString())) {
                            // if the listener is tied to SG then handle SG messages
                            handleSGMessages(vmOpenstackId, message);
                        }

                    } else if (this.entity instanceof DeploymentSpec) {
                        // / if the listener is tied to DAI which belongs to a DS then handle DAI messages
                        if (!eventType.contains(OsNotificationEventState.CREATE.toString())) {
                            // If DAI/SVA is migrated, deleted or powered off then trigger DS sync
                            handleDAIMessages(vmOpenstackId, eventType, message);
                        }

                    }
                } catch (Exception e) {
                    log.error(
                            "Fail to process Openstack VM (" + vmOpenstackId + ") notification - "
                                    + this.vc.getControllerIpAddress(), e);
                    AlertGenerator.processSystemFailureEvent(SystemFailureType.OS_NOTIFICATION_FAILURE,
                            LockObjectReference.getLockObjectReference(this.entity, new LockObjectReference(this.vc)),
                            "Fail to process Openstack VM (" + vmOpenstackId + ") notification (" + e.getMessage()
                                    + ")");
                }
            }
        }
    }

    private void handleSGMessages(String vmOpenstackId, String message) throws Exception {
        SecurityGroup securityGroup = (SecurityGroup) this.entity;
        // if the VM changes is part of Security Group
        if (!isVmMigrated(vmOpenstackId, message)) {

            /*
             * If VM is not migrated then it is deleted we must trigger a SG Sync
             */
            ConformService.startSecurityGroupConformanceJob(securityGroup);
        } else {

            /*
             * VM is migrated
             * Queue SG Sync first
             */

            try {
                // open a new Hibernate Session
                EntityManager em = HibernateUtil.getTransactionalEntityManager();
                // begin transaction
                HibernateUtil.getTransactionControl().required(() -> {
                    // load this entity from database to avoid any lazy loading issues
                    SecurityGroup sg = SecurityGroupEntityMgr.findById(em, securityGroup.getId());

                    // iterate through all SGI -> DDS mappings to trigger required DDS Sync
                    return ConformService.startSecurityGroupConformanceJob(em, sg, null, true);
                });

            } catch (ScopedWorkException e) {
                log.error("Failed to check if VM openstack Id - " + vmOpenstackId + " is migrated or not!", e.getCause());
                throw e.as(Exception.class);
            } catch (Exception e) {
                log.error("Failed to check if VM openstack Id - " + vmOpenstackId + " is migrated or not!", e);
                throw e;
            }

        }
    }

    private void handleDAIMessages(String vmOpenstackId, String eventType, String message) throws Exception {

        if (eventType.contains(OsNotificationEventState.RESIZE_CONFIRM_END.toString())) {
            if (isVmMigrated(vmOpenstackId, message)) {
                // When some one migrate DAI then we trigger sync Job to fix this issue
                ConformService.startDsConformanceJob((DeploymentSpec) this.entity, null);
            }
        } else {
            // DAI is either powered off or deleted. We must  trigger sync for this
            ConformService.startDsConformanceJob((DeploymentSpec) this.entity, null);
        }
    }

    /**
     *
     * This method verifies VM host from Openstack with one we have in our database to ensure that VM is actually
     * migrated across host.
     *
     * @param vmOpenstackId
     *            Vm Openstack ID from the Received Notification
     * @return
     *         True if Server Host ID does not match with VM Host ID i.e. VM is migrated to a new host
     *         False: if both IDs are same i.e. VM is not migrated it is just resized
     * @throws Exception
     */
    private boolean isVmMigrated(String vmOpenstackId, String message) throws Exception {
        /*
         * To verify a VM is migrated we perform the following checks
         * 1. Get Host Id from the VM in context
         * 2. Check Database and query Host Openstack ID for this VM entry
         * 3. Compare Both IDs
         * 4. If match return false
         * 5 If not same then return true as this VM is migrated to another host
         */

        try {

            VmDiscoveryCache vmCache = new VmDiscoveryCache(this.vc, this.vc.getProviderAdminTenantName());

            // parse Region from incoming Notification message
            String region = OsNotificationUtil.getPropertyFromNotificationMessage(message, REGION_NOTIFICATION_KEY);

            VmInfo vmInfo = vmCache.discover(region, vmOpenstackId);
            if (vmInfo == null) {
                log.error("Got VM notification and checking VM migration but Failed to discover VM openstack Id - '"
                        + vmOpenstackId + "'");
                return false;
            }

            EntityManager em = HibernateUtil.getTransactionalEntityManager();

            return HibernateUtil.getTransactionControl().required(() -> {
                VM vm = VMEntityManager.findByOpenstackId(em, vmOpenstackId);

                if (vm == null) {
                    log.error("Got VM notification and checking VM migration but find this VM in our DB. openstack Id - '"
                            + vmOpenstackId + "'");
                    return false;
                }

                // if the migrated VM host is same as what we have in the database then VM was resized and not Migrated
                if (!vm.getHost().equals(vmInfo.host)) {
                    return true;
                }

                return false;
            });
        } catch (ScopedWorkException e) {
            log.error("Failed to check if VM openstack Id - " + vmOpenstackId + " is migrated or not!", e.getCause());
            throw e.as(Exception.class);
        } catch (Exception e) {
            log.error("Failed to check if VM openstack Id - " + vmOpenstackId + " is migrated or not!", e);
            throw e;
        }
    }
}
