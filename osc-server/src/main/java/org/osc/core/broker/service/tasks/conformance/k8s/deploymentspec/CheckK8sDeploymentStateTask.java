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
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.k8s.KubernetesClient;
import org.osc.core.broker.rest.client.k8s.KubernetesDeployment;
import org.osc.core.broker.rest.client.k8s.KubernetesDeploymentApi;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.util.log.LogProvider;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;

@Component(service = CheckK8sDeploymentStateTask.class)
public class CheckK8sDeploymentStateTask extends TransactionalTask {
    private static final Logger LOG = LogProvider.getLogger(CheckK8sDeploymentStateTask.class);

    private DeploymentSpec ds;

    private KubernetesDeploymentApi k8sDeploymentApi;

    int MAX_RETRIES = 30;

    int RETRY_INTERVAL__MILLISECONDS = 10000;

    CheckK8sDeploymentStateTask create(DeploymentSpec ds, KubernetesDeploymentApi k8sDeploymentApi) {
        CheckK8sDeploymentStateTask task = new CheckK8sDeploymentStateTask();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;
        task.ds = ds;
        task.k8sDeploymentApi = k8sDeploymentApi;

        return task;
    }

    public CheckK8sDeploymentStateTask create(DeploymentSpec ds) {
        return create(ds, null);
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        OSCEntityManager<DeploymentSpec> dsEmgr = new OSCEntityManager<DeploymentSpec>(DeploymentSpec.class, em, this.txBroadcastUtil);
        this.ds = dsEmgr.findByPrimaryKey(this.ds.getId());

        try (KubernetesClient client = new KubernetesClient(this.ds.getVirtualSystem().getVirtualizationConnector())) {
            if (this.k8sDeploymentApi == null) {
                this.k8sDeploymentApi = new KubernetesDeploymentApi(client);
            } else {
                this.k8sDeploymentApi.setKubernetesClient(client);
            }

            int attemptCount = 0;
            for (; attemptCount < this.MAX_RETRIES; attemptCount++) {
                KubernetesDeployment k8sDeployment = this.k8sDeploymentApi.getDeploymentById(
                        this.ds.getExternalId(),
                        this.ds.getNamespace(),
                        K8sUtil.getK8sName(this.ds));

                if (k8sDeployment == null) {
                    throw new VmidcException(String.format(
                            "Kubernetes returned a null deployment for id %s, name %s, namespace %s",
                            this.ds.getExternalId(),
                            this.ds.getNamespace(),
                            K8sUtil.getK8sName(this.ds)));
                }

                if (k8sDeployment.getAvailableReplicaCount() != this.ds.getInstanceCount()) {
                    LOG.info(String.format("Kubernetes returned the deployment id %s, namespace %s and name %s with %s available count, the desired count is %s",
                            this.ds.getExternalId(),
                            this.ds.getNamespace(),
                            K8sUtil.getK8sName(this.ds),
                            k8sDeployment.getAvailableReplicaCount(),
                            this.ds.getInstanceCount()));
                } else {
                    break;
                }

                Thread.sleep(this.RETRY_INTERVAL__MILLISECONDS);
            }

            if (attemptCount == this.MAX_RETRIES) {
                throw new VmidcException("The Kubernetes deployment failed to reach the desired replica count.");
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Checking the state of the K8s deployment spec %s", this.ds.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }
}
