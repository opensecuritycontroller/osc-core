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
package org.osc.core.broker.service.tasks.conformance.manager;

import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.osc.sdk.manager.element.ManagerDeviceMemberElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = MgrCreateMemberDeviceTask.class)
public class MgrCreateMemberDeviceTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(MgrCreateMemberDeviceTask.class);

    @Reference
    private ApiFactoryService apiFactoryService;

    private DistributedApplianceInstance dai;

    public MgrCreateMemberDeviceTask create(DistributedApplianceInstance dai) {
        MgrCreateMemberDeviceTask task = new MgrCreateMemberDeviceTask();
        task.apiFactoryService = this.apiFactoryService;
        task.dai = dai;
        task.name = task.getName();
        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        this.dai = em.find(DistributedApplianceInstance.class, this.dai.getId());

        createMemberDevice(em, this.dai);
    }

    public void createMemberDevice(EntityManager em, DistributedApplianceInstance dai) throws Exception {
        ManagerDeviceApi mgrApi = this.apiFactoryService.createManagerDeviceApi(dai.getVirtualSystem());
        try {
            createMemberDevice(em, dai, mgrApi);
        } finally {
            mgrApi.close();
        }
    }

    public static void createMemberDevice(EntityManager em, DistributedApplianceInstance dai, ManagerDeviceApi mgrApi)
            throws Exception {
        try {
            String mgrDeviceId = mgrApi.createDeviceMember(dai.getName(), dai.getHostName(), dai.getIpAddress(),
                    dai.getMgmtIpAddress(), dai.getMgmtGateway(), dai.getMgmtSubnetPrefixLength());
            dai.setMgrDeviceId(mgrDeviceId);
            log.info("Create new member device (" + mgrDeviceId + ") successfully created.");

            updateApplianceConfigIfNeeded(dai, mgrApi);

            OSCEntityManager.update(em, dai);

        } catch (Exception e) {

            log.info("Failed to locate device in Manager.");
            ManagerDeviceMemberElement deviceElement = mgrApi.findDeviceMemberByName(dai.getName());
            if (deviceElement != null) {
                log.info("Found existing device (" + deviceElement.getId() + ").");

                String updatedDeviceId = mgrApi.updateDeviceMember(deviceElement, dai.getName(), dai.getHostName(), dai.getIpAddress(),
                        dai.getMgmtIpAddress(), dai.getMgmtGateway(), dai.getMgmtSubnetPrefixLength());
                dai.setMgrDeviceId(updatedDeviceId);

                updateApplianceConfigIfNeeded(dai, mgrApi);

                OSCEntityManager.update(em, dai);
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
