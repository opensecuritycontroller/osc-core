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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.service.exceptions.VmidcException;

import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;

public class KubernetesPodApiTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private org.osc.core.broker.rest.client.k8s.KubernetesClient kubernetesClient;

    @Mock
    private DefaultKubernetesClient fabric8Client;

    @Mock
    MixedOperation<Pod, PodList, DoneablePod, PodResource<Pod,DoneablePod>> operationMock;

    @Mock
    FilterWatchListDeletable<Pod, PodList, Boolean, Watch, Watcher<Pod>> filterMock;

    private KubernetesPodApi service;

    @Before
    public void testInitialize() throws Exception{
        MockitoAnnotations.initMocks(this);
        when(this.kubernetesClient.getClient()).thenReturn(this.fabric8Client);
        this.service = new KubernetesPodApi(this.kubernetesClient);

        when(this.fabric8Client.pods()).thenReturn(this.operationMock);
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

    private void mockPodsByLabel(String label, List<Pod> result) {
        when(this.operationMock.withLabel(label)).thenReturn(this.filterMock);
        when(this.filterMock.list()).thenReturn(null);
    }
}
