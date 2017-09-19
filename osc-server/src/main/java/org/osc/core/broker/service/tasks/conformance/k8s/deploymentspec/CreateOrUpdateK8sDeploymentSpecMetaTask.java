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
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.k8s.KubernetesClient;
import org.osc.core.broker.rest.client.k8s.KubernetesDeployment;
import org.osc.core.broker.rest.client.k8s.KubernetesDeploymentApi;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Creates or updates a Kubernetes deployment to satisfy the OSC deployment spec settings.
 */
@Component(service = CreateOrUpdateK8sDeploymentSpecMetaTask.class)
public class CreateOrUpdateK8sDeploymentSpecMetaTask extends TransactionalMetaTask {
    @Reference
    CreateK8sDeploymentTask createK8sDeploymentTask;

    @Reference
    UpdateK8sDeploymentTask updateK8sDeploymentTask;

    @Reference
    CheckK8sDeploymentStateTask checkK8sDeploymentStateTask;

    @Reference
    ConformK8sDeploymentPodsMetaTask conformK8sDeploymentPodsMetaTask;

    private KubernetesDeploymentApi k8sDeploymentApi;

    private DeploymentSpec ds;
    private TaskGraph tg;

    CreateOrUpdateK8sDeploymentSpecMetaTask create(DeploymentSpec ds, KubernetesDeploymentApi k8sDeploymentApi) {
        CreateOrUpdateK8sDeploymentSpecMetaTask task = new CreateOrUpdateK8sDeploymentSpecMetaTask();
        task.createK8sDeploymentTask = this.createK8sDeploymentTask;
        task.updateK8sDeploymentTask = this.updateK8sDeploymentTask;
        task.checkK8sDeploymentStateTask = this.checkK8sDeploymentStateTask;
        task.conformK8sDeploymentPodsMetaTask = this.conformK8sDeploymentPodsMetaTask;
        task.k8sDeploymentApi = k8sDeploymentApi;

        task.ds = ds;
        task.k8sDeploymentApi = this.k8sDeploymentApi;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    public CreateOrUpdateK8sDeploymentSpecMetaTask create(DeploymentSpec ds) {
        return create(ds, null);
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();
        OSCEntityManager<DeploymentSpec> dsEmgr = new OSCEntityManager<DeploymentSpec>(DeploymentSpec.class, em, this.txBroadcastUtil);
        this.ds = dsEmgr.findByPrimaryKey(this.ds.getId());

        KubernetesDeployment deployment = null;

        if (this.ds.getExternalId() != null) {
            try (KubernetesClient client = new KubernetesClient(this.ds.getVirtualSystem().getVirtualizationConnector())) {
                if (this.k8sDeploymentApi == null) {
                    this.k8sDeploymentApi = new KubernetesDeploymentApi(client);
                } else {
                    this.k8sDeploymentApi.setKubernetesClient(client);
                }

                deployment = this.k8sDeploymentApi.getDeploymentById(
                        this.ds.getExternalId(),
                        this.ds.getNamespace(),
                        K8sUtil.getK8sName(this.ds));
            }
        }

        boolean updateOrDelete = false;

        if (deployment == null) {
            this.tg.appendTask(this.createK8sDeploymentTask.create(this.ds));
            updateOrDelete = true;
        } else if (deployment.getDesiredReplicaCount() != this.ds.getInstanceCount()){
            this.tg.appendTask(this.updateK8sDeploymentTask.create(this.ds));
            updateOrDelete = true;
        }

        if (updateOrDelete) {
            this.tg.appendTask(this.checkK8sDeploymentStateTask.create(this.ds));
            this.tg.appendTask(this.conformK8sDeploymentPodsMetaTask.create(this.ds));
        }
    }

    @Override
    public String getName() {
        return String.format("Creating or updating the Kubernetes deployment spec %s", this.ds.getName());
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