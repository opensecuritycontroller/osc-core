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

import org.apache.commons.lang.StringUtils;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.sdk.manager.api.ManagerDeviceApi;
import org.osc.sdk.manager.element.ManagerDeviceMemberElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class MgrCheckDevicesMetaTask extends TransactionalMetaTask {

    @Reference
    private ApiFactoryService apiFactoryService;

    @Reference
    private MgrCreateVSSDeviceTask mgrCreateVSSDeviceTask;

    @Reference
    private MgrCreateMemberDeviceTask mgrCreateMemberDeviceTask;

    @Reference
    private MgrDeleteMemberDeviceTask mgrDeleteMemberDeviceTask;

    @Reference
    private MgrUpdateMemberDeviceTask mgrUpdateMemberDeviceTask;

    @Reference
    private MgrUpdateVSSDeviceTask mgrUpdateVSSDeviceTask;

    @Reference
    private UpdateDAISManagerDeviceId updateDAISManagerDeviceId;

    private VirtualSystem vs;
    private TaskGraph tg;

    public MgrCheckDevicesMetaTask create(VirtualSystem vs) {
        MgrCheckDevicesMetaTask task = new MgrCheckDevicesMetaTask();
        task.apiFactoryService = this.apiFactoryService;
        task.mgrCreateVSSDeviceTask = this.mgrCreateVSSDeviceTask;
        task.mgrCreateMemberDeviceTask = this.mgrCreateMemberDeviceTask;
        task.mgrDeleteMemberDeviceTask = this.mgrDeleteMemberDeviceTask;
        task.mgrUpdateMemberDeviceTask = this.mgrUpdateMemberDeviceTask;
        task.mgrUpdateMemberDeviceTask = this.mgrUpdateMemberDeviceTask;
        task.updateDAISManagerDeviceId = this.updateDAISManagerDeviceId;
        task.vs = vs;
        task.name = task.getName();
        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        this.vs = em.find(VirtualSystem.class, this.vs.getId());
        this.tg = new TaskGraph();

        try (ManagerDeviceApi mgrApi = this.apiFactoryService.createManagerDeviceApi(this.vs)) {

            // Check Container Appliance first
            if (mgrApi.isDeviceGroupSupported()) {
                checkVSSDevice(this.vs, mgrApi, em);
            }

            // TODO: Future. If deleting members tasks fails, we may still want to continue with member device checks
            // Remove dangling members

            // If device grouping not supported, then skip deleting the members on manager
            if (mgrApi.isDeviceGroupSupported() && this.vs.getMgrId() != null) {
                for (ManagerDeviceMemberElement device : mgrApi.listDeviceMembers()) {
                    DistributedApplianceInstance dai = DistributedApplianceInstanceEntityMgr.findByName(em,
                            device.getName());
                    if (dai == null) {
                        this.tg.appendTask(this.mgrDeleteMemberDeviceTask.create(this.vs, device));
                    }
                }
            }

            // Check DAIs
            if (mgrApi.isDeviceGroupSupported()) {
                for (DistributedApplianceInstance dai : this.vs.getDistributedApplianceInstances()) {
                    checkMemberDevice(em, dai, mgrApi, this.tg);
                }
            } else {
                this.tg.appendTask(this.updateDAISManagerDeviceId.create(this.vs));
            }

        }
    }

    private void checkVSSDevice(VirtualSystem vs, ManagerDeviceApi mgrApi, EntityManager em) throws Exception {

        // Check if already been created in Manager
        if (vs.getMgrId() != null) {
            this.tg.appendTask(this.mgrUpdateVSSDeviceTask.create(vs));
        } else {
            // Add device to Manager if not yet added.
            this.tg.appendTask(this.mgrCreateVSSDeviceTask.create(vs));
        }

    }

    private void checkMemberDevice(EntityManager em, DistributedApplianceInstance dai, ManagerDeviceApi mgrApi,
            TaskGraph tg) throws Exception {

        // Check if already been created in Manager
        if (dai.getMgrDeviceId() != null) {

            // Verify existence in Manager.
            tg.appendTask(this.mgrUpdateMemberDeviceTask.create(dai));
        } else {
            // Add device to Manager if not yet added.
            if (!StringUtils.isEmpty(dai.getIpAddress())) {
                tg.appendTask(this.mgrCreateMemberDeviceTask.create(dai));
            }
        }

    }

    @Override
    public String getName() {
        return "Checking Manager Devices for '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

}
