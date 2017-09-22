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
package org.osc.core.broker.service.tasks.conformance.k8s.deploymentspec;

import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This task is responsible for registering the DAI inspection port with the SDN controller.
 * If a previous inspection port already existed with a given ID the SDN controller must update it
 * if the newly provided network information. Else it must create a new one.
 */
@Component(service = RegisterK8sDAIInspectionPortTask.class)
public class RegisterK8sDAIInspectionPortTask extends TransactionalTask {
    private DistributedApplianceInstance dai;

    @Reference
    private ApiFactoryService apiFactoryService;

    public RegisterK8sDAIInspectionPortTask create(DistributedApplianceInstance dai) {
        RegisterK8sDAIInspectionPortTask task = new RegisterK8sDAIInspectionPortTask();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;
        task.apiFactoryService = this.apiFactoryService;
        task.dai = dai;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
    }

    @Override
    public String getName() {
        return String.format("Registering the inspection port for the K8s dai %s", this.dai.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }
}
