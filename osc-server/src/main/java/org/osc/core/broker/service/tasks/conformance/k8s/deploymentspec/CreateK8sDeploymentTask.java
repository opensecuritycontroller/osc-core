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
import org.osc.core.broker.rest.client.k8s.KubernetesClient;
import org.osc.core.broker.rest.client.k8s.KubernetesDeployment;
import org.osc.core.broker.rest.client.k8s.KubernetesDeploymentApi;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

@Component(service = CreateK8sDeploymentTask.class)
public class CreateK8sDeploymentTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(CreateK8sDeploymentTask.class);

    private DeploymentSpec ds;

    private KubernetesDeploymentApi k8sDeploymentApi;

    CreateK8sDeploymentTask create(DeploymentSpec ds, KubernetesDeploymentApi k8sDeploymentApi) {
        CreateK8sDeploymentTask task = new CreateK8sDeploymentTask();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;
        task.ds = ds;
        task.k8sDeploymentApi = k8sDeploymentApi;

        return task;
    }

    public CreateK8sDeploymentTask create(DeploymentSpec ds) {
        return create(ds, null);
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        OSCEntityManager<DeploymentSpec> dsEmgr = new OSCEntityManager<DeploymentSpec>(DeploymentSpec.class, em, this.txBroadcastUtil);
        this.ds = dsEmgr.findByPrimaryKey(this.ds.getId());

        KubernetesDeployment k8sDeployment = new KubernetesDeployment(
                K8sUtil.getK8sName(this.ds),
                this.ds.getNamespace(),
                null,
                this.ds.getInstanceCount(),
                this.ds.getVirtualSystem().getApplianceSoftwareVersion().getImageUrl(),
                this.ds.getVirtualSystem().getApplianceSoftwareVersion().getImagePullSecretName());

        String k8sDeploymentId = null;

        try (KubernetesClient client = new KubernetesClient(this.ds.getVirtualSystem().getVirtualizationConnector())) {
            if (this.k8sDeploymentApi == null) {
                this.k8sDeploymentApi = new KubernetesDeploymentApi(client);
            } else {
                this.k8sDeploymentApi.setKubernetesClient(client);
            }

            k8sDeploymentId = this.k8sDeploymentApi.createDeployment(k8sDeployment);
            LOG.info(String.format("Create the deployment in kubernetes with the id %s.", k8sDeploymentId));
        }

        this.ds.setExternalId(k8sDeploymentId);
        OSCEntityManager.update(em, this.ds, this.txBroadcastUtil);
    }

    @Override
    public String getName() {
        return String.format("Creating the K8s deployment spec %s", this.ds.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }
}
