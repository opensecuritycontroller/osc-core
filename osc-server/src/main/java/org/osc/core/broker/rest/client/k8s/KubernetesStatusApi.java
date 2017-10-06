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

import java.util.Optional;

import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.util.log.LogProvider;
import org.slf4j.Logger;

import io.fabric8.kubernetes.api.model.ComponentCondition;
import io.fabric8.kubernetes.api.model.ComponentStatus;
import io.fabric8.kubernetes.client.KubernetesClientException;

/**
 * This class implements the methods used by OSC to validate the availability of the Kubernetes service endpoint.
 */
public class KubernetesStatusApi extends KubernetesApi {
    private static final Logger LOG = LogProvider.getLogger(KubernetesStatusApi.class);

    // Values documented in the K8s API reference https://kubernetes.io/docs/api-reference/v1.5/#componentcondition-v1
    private static final String K8S_HEALTHY_STATUS_NAME = "Healthy";
    private static final String K8S_HEALTHY_STATUS_TRUE = "True";
    private static final String K8S_CONTROLLER_COMPONENT_NAME = "controller-manager";

    public KubernetesStatusApi(KubernetesClient client) {
        super(client.getClient());
    }

    /**
     * Determines whether the Kubernetes service endpoint is available and healthy.
     * <p>
     * In addition to checking whether the API service is reachable this method will also check the health of the Kubernetes controller-manager.
     * @return whether the K8s service endpoint is ready.
     * @throws VmidcException
     */
    public boolean isServiceReady() throws VmidcException {
        boolean result = false;
        try {
            ComponentStatus status = getKubernetesClient().componentstatuses().withName(K8S_CONTROLLER_COMPONENT_NAME).get();
            if (status == null) {
                LOG.warn(String.format("Kubernetes returned a null component for %s.", K8S_CONTROLLER_COMPONENT_NAME));
                return result;
            }

            if (status.getConditions() == null) {
                LOG.warn(String.format("Kubernetes returned null conditions for the component %s.", K8S_CONTROLLER_COMPONENT_NAME));
                return result;
            }

            Optional<ComponentCondition> healthyCondition = status.getConditions().stream().filter(condition -> condition.getType().equals(K8S_HEALTHY_STATUS_NAME)).findFirst();
            if (!healthyCondition.isPresent()) {
                LOG.warn(String.format("Kubernetes did not returned a condition with name %s for the component %s. Count of returned conditions: %s.", K8S_HEALTHY_STATUS_NAME, K8S_CONTROLLER_COMPONENT_NAME, status.getConditions().size()));
                return result;
            }

            if (healthyCondition.get().getStatus().equals(K8S_HEALTHY_STATUS_TRUE)) {
                result = true;
            } else {
                LOG.warn(String.format("Kubernetes returned a health status %s for the component %s.", healthyCondition.get().getStatus(), K8S_CONTROLLER_COMPONENT_NAME));
            }

        } catch (KubernetesClientException e) {
            throw new VmidcException("Failed to get the Kubernetes controller health status.");
        }

        return result;
    }
}
