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
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.k8s.KubernetesDeploymentApi;
import org.osc.core.broker.rest.client.k8s.KubernetesPod;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

/**
 * This task is responsible for persisting a DAI in the OSC database for a newly found pod VNF.
 *
 */
@Component(service = CreateOrUpdateK8sDAITask.class)
public class CreateOrUpdateK8sDAITask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(CreateOrUpdateK8sDAITask.class);

    private DeploymentSpec ds;

    private KubernetesPod k8sPod;

    private KubernetesDeploymentApi k8sDeploymentApi;

    public CreateOrUpdateK8sDAITask create(DeploymentSpec ds, KubernetesPod k8sPod) {
        return create(ds, k8sPod, null);
    }

    CreateOrUpdateK8sDAITask create(DeploymentSpec ds, KubernetesPod k8sPod, KubernetesDeploymentApi k8sDeploymentApi) {
        CreateOrUpdateK8sDAITask task = new CreateOrUpdateK8sDAITask();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;
        task.ds = ds;
        task.k8sPod = k8sPod;
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
        return String.format("Creating the K8s deployment spec %s and pod %s", this.ds.getName(), this.k8sPod.getUid());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }
}
