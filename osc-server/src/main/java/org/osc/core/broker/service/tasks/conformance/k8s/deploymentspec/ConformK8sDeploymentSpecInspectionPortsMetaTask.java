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

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Conforms the deployment pods for a Kubernetes deployment.
 *
 */
@Component(service = ConformK8sDeploymentSpecInspectionPortsMetaTask.class)
public class ConformK8sDeploymentSpecInspectionPortsMetaTask extends TransactionalMetaTask {
    @Reference
    DeleteK8sDAIInspectionPortTask deleteK8sDAIInspectionPortTask;

    @Reference
    RegisterK8sDAIInspectionPortTask registerK8sDAIInspectionPortTask;

    private DeploymentSpec ds;
    private TaskGraph tg;

    public ConformK8sDeploymentSpecInspectionPortsMetaTask create(DeploymentSpec ds) {
        ConformK8sDeploymentSpecInspectionPortsMetaTask task = new ConformK8sDeploymentSpecInspectionPortsMetaTask();
        task.deleteK8sDAIInspectionPortTask = this.deleteK8sDAIInspectionPortTask;
        task.registerK8sDAIInspectionPortTask = this.registerK8sDAIInspectionPortTask;
        task.ds = ds;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;
        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();
        OSCEntityManager<DeploymentSpec> dsEmgr = new OSCEntityManager<DeploymentSpec>(DeploymentSpec.class, em, this.txBroadcastUtil);
        this.ds = dsEmgr.findByPrimaryKey(this.ds.getId());

        for (DistributedApplianceInstance dai : this.ds.getDistributedApplianceInstances()) {
            // If this is an orphan DAI (not associated with a pod and its network) we must clean up the inspection port.
            if (dai.getInspectionOsIngressPortId() == null) {
                this.tg.addTask(this.deleteK8sDAIInspectionPortTask.create(dai));
            } else {
                this.tg.addTask(this.registerK8sDAIInspectionPortTask.create(dai));
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Conforming the K8s inspection ports for the deployment spec %s", this.ds.getName());
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }
}