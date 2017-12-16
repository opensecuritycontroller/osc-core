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

import java.util.ArrayList;
import java.util.List;

import org.osc.core.broker.service.exceptions.VmidcException;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClientException;

/**
 * This class implements the pod related operations used by other osc-core components.
 */
public class KubernetesPodApi extends KubernetesApi {
    public KubernetesPodApi(KubernetesClient client) {
        super(client.getClient());
    }

    private KubernetesPod getPodsByName(String namespace, String name) throws VmidcException {
        KubernetesPod resultPod = null;

        try {
            Pod pod = getKubernetesClient().pods().inNamespace(namespace).withName(name).get();

            if (pod != null) {
                resultPod = new KubernetesPod(pod.getMetadata().getName(), pod.getMetadata().getNamespace(), pod.getMetadata().getUid(),
                        pod.getSpec().getNodeName());
            }

        } catch (KubernetesClientException e) {
            throw new VmidcException("Failed to get Pods");
        }

        return resultPod;
    }

    /**
     * Retrieves all the pods labeled with the given string.
     *
     * @param label  the string label to be used to select the pods
     * @return  the list of pods selected with the provided label. Empty list if not pod is found.
     * @throws VmidcException  if a K8s SDK specific exception is caught
     */
    public List<KubernetesPod> getPodsByLabel(String label) throws VmidcException {
        List<KubernetesPod> resultPodList = new ArrayList<KubernetesPod>();

        if (label == null) {
            throw new IllegalArgumentException("Label should not be null");
        }

        try {
            PodList pods = getKubernetesClient().pods().withLabel(label).list();
            if (pods == null || pods.getItems() == null || pods.getItems().isEmpty()) {
                return resultPodList;
            }

            for (Pod pod : pods.getItems()) {
                resultPodList.add(
                        new KubernetesPod(
                                pod.getMetadata().getName(),
                                pod.getMetadata().getNamespace(),
                                pod.getMetadata().getUid(),
                                pod.getSpec().getNodeName())
                        );
            }

        } catch (KubernetesClientException e) {
            throw new VmidcException("Failed to get Pods");
        }

        return resultPodList;
    }

    /**
     * Retrieves the pod with the given uid, namespace and name
     * <p>
     * The additional parameters namespace and name are needed because K8s APIs
     * do not support queries by uid
     *
     * @param uid  the unique identifier of the pod to be retrieved
     * @param namespace  the namespace of the pod to be retrieved
     * @param name  the name of the pod to be retrieved
     * @return the pod with the given namespace, name and uid. NULL if no pod is found
     * @throws VmidcException  if a K8s SDK specific exception is caught
     */
    public KubernetesPod getPodById(String uid, String namespace, String name) throws VmidcException {
        if (uid == null) {
            throw new IllegalArgumentException("Uid should not be null");
        }

        if (name == null) {
            throw new IllegalArgumentException("Name should not be null");
        }

        if (namespace == null) {
            throw new IllegalArgumentException("Namespace should not be null");
        }

        KubernetesPod pod = getPodsByName(namespace, name);
        return (pod == null || !pod.getUid().equals(uid)) ? null : pod;
    }
}