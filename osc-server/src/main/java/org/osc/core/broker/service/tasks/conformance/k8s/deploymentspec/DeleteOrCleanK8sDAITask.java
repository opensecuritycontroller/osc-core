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

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.k8s.KubernetesDeploymentApi;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

/**
 * This task is responsible for deleting or cleaning up a DAI in the OSC database for a deleted pod.
 */
@Component(service = DeleteOrCleanK8sDAITask.class)
public class DeleteOrCleanK8sDAITask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(DeleteOrCleanK8sDAITask.class);

    private DeploymentSpec ds;

    private DistributedApplianceInstance daiForDeletion;

    private KubernetesDeploymentApi k8sDeploymentApi;

    public DeleteOrCleanK8sDAITask create(DeploymentSpec ds, DistributedApplianceInstance daiForDeletion) {
        return create(ds, daiForDeletion, null);
    }

    DeleteOrCleanK8sDAITask create(DeploymentSpec ds, DistributedApplianceInstance daiForDeletion, KubernetesDeploymentApi k8sDeploymentApi) {
        DeleteOrCleanK8sDAITask task = new DeleteOrCleanK8sDAITask();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;
        task.ds = ds;
        task.daiForDeletion = daiForDeletion;
        task.k8sDeploymentApi = k8sDeploymentApi;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        OSCEntityManager<DeploymentSpec> dsEmgr = new OSCEntityManager<DeploymentSpec>(DeploymentSpec.class, em, this.txBroadcastUtil);
        this.ds = dsEmgr.findByPrimaryKey(this.ds.getId());
    }

    @Override
    public String getName() {
        return String.format("Deleting the K8s deployment spec %s and dai %s", this.ds.getName(), this.daiForDeletion.getId());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }
}
