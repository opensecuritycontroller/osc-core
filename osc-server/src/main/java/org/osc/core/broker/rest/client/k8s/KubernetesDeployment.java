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

import io.fabric8.kubernetes.api.model.extensions.Deployment;

public class KubernetesDeployment extends KubernetesEntity {
    private String namespace;
    private int desiredReplicaCount;
    private int availableReplicaCount;
    private String containerImageName;
    private String imagePullSecretName;
    private Deployment deploymentResource;

    public KubernetesDeployment(String name, String namespace, String uid, int desiredReplicaCount, String containerImageName, String imagePullSecretName) {
        super(name, uid);
        this.namespace = namespace;
        this.desiredReplicaCount = desiredReplicaCount;
        this.containerImageName = containerImageName;
        this.imagePullSecretName = imagePullSecretName;
    }

    public int getDesiredReplicaCount() {
        return this.desiredReplicaCount;
    }

    public int getAvailableReplicaCount() {
        return this.availableReplicaCount;
    }

    void setAvailableReplicaCount(int availableReplicaCount) {
        this.availableReplicaCount = availableReplicaCount;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String getContainerImageName() {
        return this.containerImageName;
    }

    public String getImagePullSecretName() {
        return this.imagePullSecretName;
    }

    Deployment getDeploymentResource() {
        return this.deploymentResource;
    }

    void setDeploymentResource(Deployment deploymentResource) {
        this.deploymentResource = deploymentResource;
    }
}
