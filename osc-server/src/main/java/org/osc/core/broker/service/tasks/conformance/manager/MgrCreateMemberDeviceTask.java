/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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
package org.osc.core.broker.service.tasks.conformance.manager;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.osc.sdk.manager.element.ManagerDeviceMemberElement;

public class MgrCreateMemberDeviceTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(MgrCreateMemberDeviceTask.class);

    private DistributedApplianceInstance dai;

    public MgrCreateMemberDeviceTask(DistributedApplianceInstance dai) {
        this.dai = dai;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        this.dai = (DistributedApplianceInstance) session.get(DistributedApplianceInstance.class, this.dai.getId());

        createMemberDevice(session, this.dai);
    }

    public static void createMemberDevice(Session session, DistributedApplianceInstance dai) throws Exception {
        ManagerDeviceApi mgrApi = ManagerApiFactory.createManagerDeviceApi(dai.getVirtualSystem());
        try {
            createMemberDevice(session, dai, mgrApi);
        } finally {
            mgrApi.close();
        }
    }

    public static void createMemberDevice(Session session, DistributedApplianceInstance dai, ManagerDeviceApi mgrApi)
            throws Exception {
        try {
            String mgrDeviceId = mgrApi.createDeviceMember(dai.getName(), dai.getHostName(), dai.getIpAddress(),
                    dai.getMgmtIpAddress(), dai.getMgmtGateway(), dai.getMgmtSubnetPrefixLength());
            dai.setMgrDeviceId(mgrDeviceId);
            log.info("Create new member device (" + mgrDeviceId + ") successfully created.");

            updateApplianceConfigIfNeeded(dai, mgrApi);

            EntityManager.update(session, dai);

        } catch (Exception e) {

            log.info("Failed to locate device in Manager.");
            ManagerDeviceMemberElement deviceElement = mgrApi.findDeviceMemberByName(dai.getName());
            if (deviceElement != null) {
                log.info("Found existing device (" + deviceElement.getId() + ").");

                String updatedDeviceId = mgrApi.updateDeviceMember(deviceElement, dai.getName(), dai.getHostName(), dai.getIpAddress(),
                        dai.getMgmtIpAddress(), dai.getMgmtGateway(), dai.getMgmtSubnetPrefixLength());
                dai.setMgrDeviceId(updatedDeviceId);

                updateApplianceConfigIfNeeded(dai, mgrApi);

                EntityManager.update(session, dai);
            } else {
                throw e;
            }
        }
    }

    private static void updateApplianceConfigIfNeeded(DistributedApplianceInstance dai, ManagerDeviceApi mgrApi)
            throws Exception {
        if (dai.getApplianceConfig() == null) {
            byte[] applianceConfig = mgrApi.getDeviceMemberConfigById(dai.getMgrDeviceId());
            dai.setApplianceConfig(applianceConfig);
            if (applianceConfig != null) {
                log.info("Got appliance config for member device (" + dai.getMgrDeviceId() + ").");
            }
        }
    }

    @Override
    public String getName() {
        return "Create Manager Member Device for '" + this.dai.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
