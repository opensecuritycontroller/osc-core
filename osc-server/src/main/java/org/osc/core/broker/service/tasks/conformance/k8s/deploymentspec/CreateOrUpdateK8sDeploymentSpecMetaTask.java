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
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osgi.service.component.annotations.Component;

/**
 * Conforms the deployment spec for Kubernetes according to its settings. This task assumes a Lock has been placed on the DS by the job
 * containing this task.
 */
@Component(service = CreateOrUpdateK8sDeploymentSpecMetaTask.class)
public class CreateOrUpdateK8sDeploymentSpecMetaTask extends TransactionalMetaTask {
    private DeploymentSpec ds;
    private TaskGraph tg;

    public CreateOrUpdateK8sDeploymentSpecMetaTask create(DeploymentSpec ds) {
        CreateOrUpdateK8sDeploymentSpecMetaTask task = new CreateOrUpdateK8sDeploymentSpecMetaTask();
        task.ds = ds;
        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
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