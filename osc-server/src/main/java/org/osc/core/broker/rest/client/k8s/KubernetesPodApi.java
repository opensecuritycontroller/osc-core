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

import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.exceptions.VmidcException;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClientException;

/**
 * This class implements the pod related operations used by other osc-core
 * components.
 */
public class KubernetesPodApi extends KubernetesApi {
	public KubernetesPodApi(VirtualizationConnector vc) {
		super(vc);
	}

	/**
	 * Returns Pods by name and namespace.
	 * 
	 * @param namespace
	 *            Pods namespace
	 * @param name
	 *            Pods name
	 *            <p>
	 *            Each string on this set must have the format "key=value" and
	 *            all the keys must be the same.
	 * @return return pods that matches mane and namespace, empty string if not
	 *         found
	 * @throws KubernetesClientException
	 *             exception is caught
	 */
	private KubernetesPod getPodsByName(String namespace, String name) throws VmidcException {
		KubernetesPod kPod = null;

		try {
			Pod pod = this.getKubernetesClient().pods().inNamespace(namespace).withName(name).get();

			if (pod != null) {
				kPod = new KubernetesPod(pod.getMetadata().getName(), namespace, pod.getMetadata().getUid(),
						pod.getSpec().getNodeName());
			}

		} catch (KubernetesClientException e) {
			throw new VmidcException("Failed to get Pods");
		}

		return kPod;
	}

	/**
	 * Returns all the pods for a give label
	 * 
	 * @param label
	 *            the string label to be used to select the pods
	 *            <p>
	 *            string on this must have the format "key=value" and all the
	 *            keys must be the same.
	 * @return the list of pods with any of the provided labels, empty list if
	 *         no pod is found
	 * @throws VmidcException
	 *             if a K8s SDK specific exception is caught
	 */
	public List<KubernetesPod> getPodsByLabel(String label) throws VmidcException {
		List<KubernetesPod> kPodList = new ArrayList<KubernetesPod>();

		if (label == null) {
			throw new IllegalArgumentException("Label should not be null");
		}

		try {
			PodList pods = this.getKubernetesClient().pods().withLabel(label).list();
			if (pods != null && !(pods.getItems().isEmpty())) {
				for (Pod pod : pods.getItems()) {
					kPodList.add(new KubernetesPod(pod.getMetadata().getName(), pod.getMetadata().getNamespace(),
							pod.getMetadata().getUid(), pod.getSpec().getNodeName()));
				}
			}
		} catch (KubernetesClientException e) {
			throw new VmidcException("Failed to get Pods");
		}
		return kPodList;
	}

	/**
	 * Returns the pod with the given uid, namespace and name
	 * <p>
	 * The additional parameters namespace and name are needed because K8s APIs
	 * do not support queries by uid
	 * 
	 * @param uid
	 *            the unique identifier of the pod to be retrieved
	 * @param namespace
	 *            the namespace of the pod to be retrieved
	 * @param name
	 *            the name of the pod to be retrieved
	 * @return the pod with the given namespace, name and uid, NULL if no pod is
	 *         found
	 * @throws VmidcException
	 *             if a K8s SDK specific exception is caught
	 */
	public KubernetesPod getPodById(String uid, String namespace, String name) throws VmidcException {
		if (uid == null) {
			throw new IllegalArgumentException("Uid Should not be null");
		} else if (name == null) {
			throw new IllegalArgumentException("Name Should not be null");
		} else if (namespace == null) {
			throw new IllegalArgumentException("namespace Should not be null");
		}
		KubernetesPod kPod = getPodsByName(namespace, name);
		return (kPod == null || !kPod.getUid().equals(uid)) ? null : kPod;
	}

}