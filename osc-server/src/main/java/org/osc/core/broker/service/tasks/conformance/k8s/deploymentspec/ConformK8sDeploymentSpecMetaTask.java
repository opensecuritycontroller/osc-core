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
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.IgnoreCompare;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.DeleteDSFromDbTask;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Conforms the deployment spec for Kubernetes according to its settings.
 * <p>
 * This task assumes a Lock has been placed on the DS by the job containing this task.
 */
@Component(service = ConformK8sDeploymentSpecMetaTask.class)
public class ConformK8sDeploymentSpecMetaTask extends TransactionalMetaTask {
    @Reference
    DeleteK8sDeploymentTask deleteK8sDeploymentTask;

    @Reference
    CreateOrUpdateK8sDeploymentSpecMetaTask createOrUpdateK8sDeploymentSpecMetaTask;

    @Reference
    DeleteK8sDAIInspectionPortTask deleteK8sDAIInspectionPortTask;

    @Reference
    MgrCheckDevicesMetaTask mgrCheckDevicesMetaTask;

    @Reference
    DeleteDSFromDbTask deleteDSFromDbTask;

    private DeploymentSpec ds;
    private TaskGraph tg;

    @IgnoreCompare
    private AtomicBoolean initDone = new AtomicBoolean();

    public ConformK8sDeploymentSpecMetaTask create(DeploymentSpec ds) {
        ConformK8sDeploymentSpecMetaTask task = new ConformK8sDeploymentSpecMetaTask();
        task.deleteK8sDAIInspectionPortTask = this.deleteK8sDAIInspectionPortTask;
        task.deleteK8sDeploymentTask = this.deleteK8sDeploymentTask;
        task.createOrUpdateK8sDeploymentSpecMetaTask = this.createOrUpdateK8sDeploymentSpecMetaTask;
        task.mgrCheckDevicesMetaTask = this.mgrCheckDevicesMetaTask;
        task.deleteDSFromDbTask = this.deleteDSFromDbTask;
        task.ds = ds;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;
        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        delayedInit();
        this.tg = new TaskGraph();

        OSCEntityManager<DeploymentSpec> dsEmgr = new OSCEntityManager<DeploymentSpec>(DeploymentSpec.class, em, this.txBroadcastUtil);
        this.ds = dsEmgr.findByPrimaryKey(this.ds.getId());

        VirtualSystem virtualSystem = this.ds.getVirtualSystem();

        if (!this.ds.getMarkedForDeletion() && !virtualSystem.getMarkedForDeletion()
                && !virtualSystem.getDistributedAppliance().getMarkedForDeletion()) {
            this.tg.appendTask(this.createOrUpdateK8sDeploymentSpecMetaTask.create(this.ds));
        } else {
            this.tg.appendTask(this.deleteK8sDeploymentTask.create(this.ds));

            for(DistributedApplianceInstance dai : this.ds.getDistributedApplianceInstances()) {
                this.tg.addTask(this.deleteK8sDAIInspectionPortTask.create(dai));
            }

            this.tg.appendTask(this.mgrCheckDevicesMetaTask.create(this.ds.getVirtualSystem()));
            this.tg.appendTask(this.deleteDSFromDbTask.create(this.ds));
        }
    }

    @Override
    public String getName() {
        return String.format("Checking Deployment Spec '%s'", this.ds.getName());
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