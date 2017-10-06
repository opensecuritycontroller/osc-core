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

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.util.log.LogProvider;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.osc.sdk.manager.element.ManagerDeviceMemberElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

/**
 * Updates the device member and subsequently updates the respective distributed appliance instance with the identifier of the updated device member.
 */
@Component(service = MgrUpdateMemberDeviceTask.class)
public class MgrUpdateMemberDeviceTask extends TransactionalTask {
    private static final Logger log = LogProvider.getLogger(MgrUpdateMemberDeviceTask.class);

    @Reference
    private ApiFactoryService apiFactoryService;

    private DistributedApplianceInstance dai;

    public MgrUpdateMemberDeviceTask create(DistributedApplianceInstance dai) {
        MgrUpdateMemberDeviceTask task = new MgrUpdateMemberDeviceTask();
        task.apiFactoryService = this.apiFactoryService;
        task.dai = dai;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        this.dai = em.find(DistributedApplianceInstance.class, this.dai.getId());

        ManagerDeviceApi mgrApi = this.apiFactoryService.createManagerDeviceApi(this.dai.getVirtualSystem());

        try {
            ManagerDeviceMemberElement deviceElement = mgrApi.getDeviceMemberById(this.dai.getMgrDeviceId());

            String updatedDeviceId = mgrApi.updateDeviceMember(deviceElement, this.dai.getName(), this.dai.getHostName(),
                    this.dai.getIpAddress(), this.dai.getMgmtIpAddress(), this.dai.getMgmtGateway(), this.dai.getMgmtSubnetPrefixLength());
            this.dai.setMgrDeviceId(updatedDeviceId);

            updateApplianceConfigIfNeeded(this.dai, mgrApi);

            OSCEntityManager.update(em, this.dai, this.txBroadcastUtil);

        } catch (Exception e) {

            log.info("Failed to locate device member in Manager.");
            ManagerDeviceMemberElement deviceElement = mgrApi.findDeviceMemberByName(this.dai.getName());
            if (deviceElement != null) {
                log.info("Member device ID " + this.dai.getMgrDeviceId() + " was located by '" + this.dai.getName() + "' and ID "
                        + deviceElement.getId());
                String updatedDeviceId = mgrApi.updateDeviceMember(deviceElement, this.dai.getName(), this.dai.getHostName(),
                        this.dai.getIpAddress(), this.dai.getMgmtIpAddress(), this.dai.getMgmtGateway(),
                        this.dai.getMgmtSubnetPrefixLength());
                this.dai.setMgrDeviceId(updatedDeviceId);
            } else {
                this.dai.setMgrDeviceId(null);
            }

            updateApplianceConfigIfNeeded(this.dai, mgrApi);
            OSCEntityManager.update(em, this.dai, this.txBroadcastUtil);
        } finally {
            mgrApi.close();
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
        return "Update Manager Member Device for '" + this.dai.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
