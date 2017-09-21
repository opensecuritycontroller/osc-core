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
package org.osc.core.broker.rest.client.k8s;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.exceptions.VmidcException;

import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;

/**
 * This class implements the pod related operations used by other
 * osc-core components.
 */
public class KubernetesDeploymentApi extends KubernetesApi {
    public final static String OSC_DEPLOYMENT_LABEL_NAME = "osc-deployment";

    private static final Logger LOG = Logger.getLogger(KubernetesDeploymentApi.class);

    public KubernetesDeploymentApi(KubernetesClient client) {
        super(client.getClient());
    }

    /**
     * Retrieves the deployment with the given uid, namespace and name
     * <p>
     * The additional parameters namespace and name are needed because K8s APIs
     * do not support queries by uid
     *
     * @param uid  the unique identifier of the deployment to be retrieved
     * @param namespace  the namespace of the deployment to be retrieved
     * @param name  the name of the deployment to be retrieved
     * @return the deployment with the given namespace, name and uid. NULL if no deployment is found
     * @throws VmidcException  if a K8s SDK specific exception is caught
     */
    public KubernetesDeployment getDeploymentById(String uid, String namespace, String name) throws VmidcException {
        if (uid == null) {
            throw new IllegalArgumentException("Uid should not be null");
        }

        if (name == null) {
            throw new IllegalArgumentException("Name should not be null");
        }

        if (namespace == null) {
            throw new IllegalArgumentException("Namespace should not be null");
        }

        KubernetesDeployment deployment = getDeploymentByName(namespace, name);
        return (deployment == null || !deployment.getUid().equals(uid)) ? null : deployment;
    }

    /**
     * Deletes the deployment with the given uid, namespace and name. If the targeted deployment is not found, no-op.
     * <p>
     * The additional parameters namespace and name are needed because K8s APIs
     * do not support queries by uid
     *
     * @param uid  the unique identifier of the deployment to be deleted
     * @param namespace  the namespace of the deployment to be deleted
     * @param name  the name of the deployment to be deleted
     * @throws VmidcException  if a K8s SDK specific exception is caught
     */
    public void deleteDeployment(String uid, String namespace, String name) throws VmidcException {
        if (uid == null) {
            throw new IllegalArgumentException("Uid should not be null");
        }

        if (name == null) {
            throw new IllegalArgumentException("Name should not be null");
        }

        if (namespace == null) {
            throw new IllegalArgumentException("Namespace should not be null");
        }

        KubernetesDeployment deployment = getDeploymentById(uid, namespace, name);
        if (deployment == null) {
            LOG.info(String.format("The deployment with id %s, name %s and namespace %s was not found. Nothing to do.", uid, name, namespace));
            return;
        }

        try {
            getKubernetesClient().resource(deployment.getDeploymentResource()).delete();
        } catch (KubernetesClientException e) {
            throw new VmidcException("Failed to delete the deployment");
        }
    }

    /**
     * Updates the desired replica count of the deployment with the given uid, namespace and name.
     * <p>
     * The additional parameters namespace and name are needed because K8s APIs
     * do not support queries by uid
     *
     * @param uid  the unique identifier of the deployment to be updated
     * @param namespace  the namespace of the deployment to be updated
     * @param name  the name of the deployment to be updated
     * @throws VmidcException  if a K8s SDK specific exception is caught
     */
    public void updateDeploymentReplicaCount(String uid, String namespace, String name, int replicaCount) throws VmidcException {
        if (uid == null) {
            throw new IllegalArgumentException("The uid should not be null");
        }

        if (name == null) {
            throw new IllegalArgumentException("The name should not be null");
        }

        if (namespace == null) {
            throw new IllegalArgumentException("The namespace should not be null");
        }

        if (replicaCount < 1) {
            throw new IllegalArgumentException("The replica count should not be smaller than 1.");
        }

        KubernetesDeployment deployment = getDeploymentById(uid, namespace, name);
        if (deployment == null) {
            throw new VmidcException(String.format("The deployment with id %s, name %s and namespace %s was not found during update.", uid, name, namespace));
        }

        try {
            getKubernetesClient().extensions().deployments().inNamespace(namespace).withName(name).scale(replicaCount);
        } catch (KubernetesClientException e) {
            throw new VmidcException("Failed to update the deployment");
        }
    }

    /**
     * Creates a Kubernetes deployment.
     *
     * @param deployment  the deployment to be created
     * @return  the unique identifier of the deployment
     * @throws VmidcException  if a K8s SDK specific exception is caught
     */
    public String createDeployment(KubernetesDeployment deployment) throws VmidcException {
        if (deployment == null) {
            throw new IllegalArgumentException("The deployment should not be null");
        }

        try {
            Deployment newDeployment = new DeploymentBuilder()
                    .withKind("Deployment")
                    .withNewMetadata()
                    .withName(deployment.getName())
                    .endMetadata()
                    .withNewSpec()
                    .withReplicas(deployment.getDesiredReplicaCount())
                    .withNewTemplate()
                    .withNewMetadata()
                    .addToLabels(OSC_DEPLOYMENT_LABEL_NAME, deployment.getName())
                    .endMetadata()
                    .withNewSpec()
                    .addNewContainer()
                    .withName(deployment.getName()).withImage(deployment.getContainerImageName())
                    .endContainer()
                    .endSpec()
                    .endTemplate()
                    .endSpec()
                    .build();

            Deployment result = getKubernetesClient().extensions().deployments().inNamespace(deployment.getNamespace()).create(newDeployment);

            return result.getMetadata().getUid();

        } catch (KubernetesClientException e) {
            throw new VmidcException(String.format("Failed to create a deployment %s", e));
        }
    }

    private KubernetesDeployment getDeploymentByName(String namespace, String name) throws VmidcException {
        KubernetesDeployment resultDeployment = null;

        try {
            Deployment deployment = getKubernetesClient().extensions().deployments().inNamespace(namespace).withName(name).get();

            if (deployment != null) {
                resultDeployment = new KubernetesDeployment(
                        deployment.getMetadata().getName(),
                        deployment.getMetadata().getNamespace(),
                        deployment.getMetadata().getUid(),
                        deployment.getSpec().getReplicas(),
                        deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getImage(),
                        deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getImagePullPolicy());
                resultDeployment.setDeploymentResource(deployment);
                resultDeployment.setAvailableReplicaCount(deployment.getStatus().getAvailableReplicas());
            }

        } catch (KubernetesClientException e) {
            throw new VmidcException("Failed to get the deployment");
        }

        return resultDeployment;
    }
}
