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

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.service.exceptions.VmidcException;

import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;

public class KubernetesPodApiTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private org.osc.core.broker.rest.client.k8s.KubernetesClient kubernetesClient;

    @Mock
    private DefaultKubernetesClient fabric8Client;

    @Mock
    MixedOperation<Pod, PodList, DoneablePod, PodResource<Pod,DoneablePod>> mixedOperationMock;

    @Mock
    NonNamespaceOperation<Pod, PodList, DoneablePod, PodResource<Pod,DoneablePod>> nonNamespaceOperationMock;

    @Mock
    FilterWatchListDeletable<Pod, PodList, Boolean, Watch, Watcher<Pod>> filterMock;

    private KubernetesPodApi service;

    @Before
    public void testInitialize() throws Exception{
        MockitoAnnotations.initMocks(this);
        when(this.kubernetesClient.getClient()).thenReturn(this.fabric8Client);
        this.service = new KubernetesPodApi(this.kubernetesClient);

        when(this.fabric8Client.pods()).thenReturn(this.mixedOperationMock);
    }

    @Test
    public void testGetPodsbyLabel_WithNullLabel_ThrowsIllegalArgumentException() throws Exception {
        // Arrange.
        this.exception.expect(IllegalArgumentException.class);

        // Act.
        this.service.getPodsByLabel(null);
    }

    @Test
    public void testGetPodsbyLabel_WhenK8ClientThrowsKubernetesClientException_ThrowsVmidcException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcException.class);
        when(this.fabric8Client.pods()).thenThrow(new KubernetesClientException(""));

        // Act.
        this.service.getPodsByLabel("sample_label");
    }

    @Test
    public void testGetPodsbyLabel_WhenK8sReturnsNull_ReturnsEmptyList() throws Exception {
        // Arrange.
        String label = UUID.randomUUID().toString();
        mockPodsByLabel(label, null);

        // Act.
        List<KubernetesPod> result = this.service.getPodsByLabel(label);

        // Assert.
        assertNotNull("The result should not be null.", result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetPodsbyLabel_WhenK8sReturnsEmptyList_ReturnsEmptyList() throws Exception {
        // Arrange.
        String label = UUID.randomUUID().toString();
        mockPodsByLabel(label, new ArrayList<Pod>());

        // Act.
        List<KubernetesPod> result = this.service.getPodsByLabel(label);

        // Assert.
        assertNotNull("The result should not be null.", result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetPodsbyLabel_WhenK8sReturnsSinglePod_ReturnsSinglePod() throws Exception {
        testGetPodsbyLabel_WhenK8sReturnsPods(1);
    }

    @Test
    public void testGetPodsbyLabel_WhenK8sReturnsMultiplePods_ReturnsMultiplePods() throws Exception {
        testGetPodsbyLabel_WhenK8sReturnsPods(3);
    }

    @Test
    public void testGetPodsbyId_WithNullName_ThrowsIllegalArgumentException() throws Exception {
        // Arrange.
        this.exception.expect(IllegalArgumentException.class);

        // Act.
        this.service.getPodById("1234", null, "sample_namespace");
    }

    @Test
    public void testGetPodsbyId_WithNullUid_ThrowsIllegalArgumentException() throws Exception {
        // Arrange.
        this.exception.expect(IllegalArgumentException.class);

        // Act.
        this.service.getPodById(null, "sample_name", "sample_namespace");
    }

    @Test
    public void testGetPodsbyId_WithNullNameSpace_ThrowsIllegalArgumentException() throws Exception {
        // Arrange.
        this.exception.expect(IllegalArgumentException.class);

        // Act.
        this.service.getPodById("1234", "sample_name", null);
    }

    @Test
    public void testGetPodById_WhenK8ClientThrowsKubernetesClientException_ThrowsVmidcException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcException.class);
        when(this.fabric8Client.pods()).thenThrow(new KubernetesClientException(""));

        // Act.
        this.service.getPodById("1234", "sample_name", "sample_label");
    }

    @Test
    public void testGetPodsbyId_WhenK8sReturnsNull_ReturnsNull() throws Exception {
        // Arrange.
        String name = UUID.randomUUID().toString();
        String namespace = UUID.randomUUID().toString();
        mockPodsByName(namespace, name, null);

        // Act.
        KubernetesPod result = this.service.getPodById(UUID.randomUUID().toString(), namespace, name);

        // Assert.
        assertNull("The result should be null.", result);
    }

    @Test
    public void testGetPodsbyLabel_WhenK8sReturnsPodWithMismatchingId_ReturnsEmptyList() throws Exception {
        // Arrange.
        String name = UUID.randomUUID().toString();
        String namespace = UUID.randomUUID().toString();
        mockPodsByName(namespace, name, newPod(UUID.randomUUID().toString(), name, namespace, "node"));

        // Act.
        KubernetesPod result = this.service.getPodById(UUID.randomUUID().toString(), namespace, name);

        // Assert.
        assertNull("The result should be null.", result);
    }

    @Test
    public void testGetPodsbyLabel_WhenK8sReturnsPodWithMatchingId_ReturnsPod() throws Exception {
        // Arrange.
        String name = UUID.randomUUID().toString();
        String namespace = UUID.randomUUID().toString();
        String uid = UUID.randomUUID().toString();
        Pod pod = newPod(uid, namespace, name, UUID.randomUUID().toString());
        mockPodsByName(namespace, name, pod);

        // Act.
        KubernetesPod result = this.service.getPodById(uid, namespace, name);

        // Assert.
        assertNotNull("The result should not be null.", result);
        assertPodFields(pod, result);
    }

    private void mockPodsByLabel(String label, List<Pod> result) {
        when(this.mixedOperationMock.withLabel(label)).thenReturn(this.filterMock);
        PodList podList = new PodList();
        podList.setItems(result);
        when(this.filterMock.list()).thenReturn(podList);
    }

    private void mockPodsByName(String namespace, String name, Pod pod) {
        when(this.mixedOperationMock.inNamespace(namespace)).thenReturn(this.nonNamespaceOperationMock);
        @SuppressWarnings("unchecked")
        PodResource<Pod, DoneablePod> podResource = Mockito.mock(PodResource.class);
        when(podResource.get()).thenReturn(pod);

        when(this.nonNamespaceOperationMock.withName(name)).thenReturn(podResource);
    }

    private Pod newPod(String uid, String namespace, String name, String node) {
        Pod pod = new Pod();

        ObjectMeta objMeta = new ObjectMeta();
        objMeta.setName(name);
        objMeta.setNamespace(namespace);
        objMeta.setUid(uid);

        PodSpec spec = new PodSpec();
        spec.setNodeName(node);

        pod.setMetadata(objMeta);
        pod.setSpec(spec);

        return pod;
    }

    private void assertPodFields(Pod expectedPod, KubernetesPod actualPod) {
        assertEquals("The pod name was different than the expected.", expectedPod.getMetadata().getName(), actualPod.getName());
        assertEquals("The pod namespace was different than the expected.", expectedPod.getMetadata().getNamespace(), actualPod.getNamespace());
        assertEquals("The pod uid was different than the expected.", expectedPod.getMetadata().getUid(), actualPod.getUid());
        assertEquals("The pod node name was different than the expected.", expectedPod.getSpec().getNodeName(), actualPod.getNode());
    }

    private void assertPodsList(List<Pod> expectedPods, List<KubernetesPod> actualPods) {
        assertEquals("The size of the pods list was different than expected.", expectedPods.size(), actualPods.size());

        expectedPods.sort((p1, p2) -> p1.getMetadata().getUid().compareTo(p2.getMetadata().getUid()));
        actualPods.sort((p1, p2) -> p1.getUid().compareTo(p2.getUid()));
        int index = 0;

        for (Pod expectedPod : expectedPods) {
            assertPodFields(expectedPod, actualPods.get(index));
            index++;
        }
    }

    private void testGetPodsbyLabel_WhenK8sReturnsPods(int podCount) throws Exception {
        // Arrange.
        String label = UUID.randomUUID().toString();

        List<Pod> pods = new ArrayList<>();

        for(int i = 0; i < podCount; i++) {
            pods.add(newPod(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        }

        mockPodsByLabel(label, pods);

        // Act.
        List<KubernetesPod> result = this.service.getPodsByLabel(label);

        // Assert.
        assertNotNull("The result should not be null.", result);
        assertPodsList(pods, result);

    }
}
