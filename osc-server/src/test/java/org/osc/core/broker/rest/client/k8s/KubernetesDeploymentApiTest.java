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
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.service.exceptions.VmidcException;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentList;
import io.fabric8.kubernetes.api.model.extensions.DeploymentSpec;
import io.fabric8.kubernetes.api.model.extensions.DeploymentStatus;
import io.fabric8.kubernetes.api.model.extensions.DoneableDeployment;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.ExtensionsAPIGroupDSL;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NamespaceVisitFromServerGetWatchDeleteRecreateWaitApplicable;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.ScalableResource;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class KubernetesDeploymentApiTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private org.osc.core.broker.rest.client.k8s.KubernetesClient kubernetesClient;

    @Mock
    private DefaultKubernetesClient fabric8Client;

    @Mock
    MixedOperation<Deployment, DeploymentList, DoneableDeployment, ScalableResource<Deployment,DoneableDeployment>> mixedOperationMock;

    @Mock
    NonNamespaceOperation<Deployment, DeploymentList, DoneableDeployment, ScalableResource<Deployment,DoneableDeployment>> nonNamespaceOperationMock;

    @Mock
    ExtensionsAPIGroupDSL extensionsApiMock;

    @Mock
    NamespaceVisitFromServerGetWatchDeleteRecreateWaitApplicable<HasMetadata, Boolean> deploymentMock;

    @Mock
    FilterWatchListDeletable<Pod, PodList, Boolean, Watch, Watcher<Pod>> filterMock;

    private KubernetesDeploymentApi deploymentApi;

    @Before
    public void testInitialize() throws Exception{
        MockitoAnnotations.initMocks(this);
        when(this.kubernetesClient.getClient()).thenReturn(this.fabric8Client);
        this.deploymentApi = new KubernetesDeploymentApi(this.kubernetesClient);

        when(this.fabric8Client.extensions()).thenReturn(this.extensionsApiMock);
        when(this.fabric8Client.resource(Mockito.any(Deployment.class))).thenReturn(this.deploymentMock);
        when(this.extensionsApiMock.deployments()).thenReturn(this.mixedOperationMock);
    }

    @Test
    @Parameters(method = "getNullInputsTestData")
    public void testDeploymentApis_WithNullInputs_ThrowsIllegalArgumentException(String uuid, String namespace, String name, DeploymentOperation operation) throws Exception {
        // Arrange.
        this.exception.expect(IllegalArgumentException.class);

        // Act.
        switch (operation) {

        case GET :
            this.deploymentApi.getDeploymentById(uuid, namespace, name);
            break;
        case UPDATE :
            this.deploymentApi.updateDeploymentReplicaCount(uuid, namespace, name, 1);
            break;

        case DELETE :
            this.deploymentApi.deleteDeployment(uuid, namespace, name);
            break;

        default:
            throw new IllegalArgumentException("");
        }
    }

    @Test
    public void testGetDeploymentbyId_WhenK8ClientThrowsKubernetesClientException_ThrowsVmidcException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcException.class);
        when(this.extensionsApiMock.deployments()).thenThrow(new KubernetesClientException(""));

        // Act.
        this.deploymentApi.getDeploymentById("1234", "sample_name", "sample_label");
    }

    @Test
    public void testGetDeploymentbyId_WhenK8sReturnsNull_ReturnsNull() throws Exception {
        // Arrange.
        String name = UUID.randomUUID().toString();
        String namespace = UUID.randomUUID().toString();
        mockDeploymentByName(namespace, name, null);

        // Act.
        KubernetesDeployment result = this.deploymentApi.getDeploymentById(UUID.randomUUID().toString(), namespace, name);

        // Assert.
        assertNull("The result should be null.", result);
    }

    @Test
    public void testGetDeploymentById_WhenK8sReturnsDeploymentWithMismatchingId_ReturnsNull() throws Exception {
        // Arrange.
        String name = UUID.randomUUID().toString();
        String namespace = UUID.randomUUID().toString();
        mockDeploymentByName(namespace, name, newDeployment(UUID.randomUUID().toString(), name, namespace, 1, "image-sample-name"));

        // Act.
        KubernetesDeployment result = this.deploymentApi.getDeploymentById(UUID.randomUUID().toString(), namespace, name);

        // Assert.
        assertNull("The result should be null.", result);
    }

    @Test
    public void testGetDeploymentById_WhenK8sReturnsDeploymentWithMatchingId_ReturnsDeployment() throws Exception {
        // Arrange.
        String name = UUID.randomUUID().toString();
        String namespace = UUID.randomUUID().toString();
        String uid = UUID.randomUUID().toString();
        Deployment deployment = newDeployment(uid, namespace, name, 1, "image-sample-name");
        mockDeploymentByName(namespace, name, deployment);

        // Act.
        KubernetesDeployment result = this.deploymentApi.getDeploymentById(uid, namespace, name);

        // Assert.
        assertNotNull("The result should not be null.", result);
        assertDeploymentFields(deployment, result);
    }

    @Test
    public void testDeleteDeployment_WhenK8ClientThrowsKubernetesClientException_ThrowsVmidcException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcException.class);
        when(this.extensionsApiMock.deployments()).thenThrow(new KubernetesClientException(""));

        // Act.
        this.deploymentApi.deleteDeployment("1234", "sample_name", "sample_label");
    }

    @Test
    public void testDeleteDeployment_WhenK8sReturnsNull_DoesNothing() throws Exception {
        // Arrange.
        String name = UUID.randomUUID().toString();
        String namespace = UUID.randomUUID().toString();
        mockDeploymentByName(namespace, name, null);

        // Act.
        this.deploymentApi.deleteDeployment(UUID.randomUUID().toString(), namespace, name);

        // Assert.
        verify(this.deploymentMock, never()).delete();
    }

    @Test
    public void testDeleteDeployment_WhenK8sReturnsDeploymentWithMismatchingId_DoesNothing() throws Exception {
        // Arrange.
        String name = UUID.randomUUID().toString();
        String namespace = UUID.randomUUID().toString();
        mockDeploymentByName(namespace, name, newDeployment(UUID.randomUUID().toString(), name, namespace, 1, "image-sample-name"));

        // Act.
        this.deploymentApi.deleteDeployment(UUID.randomUUID().toString(), namespace, name);

        // Assert.
        verify(this.deploymentMock, never()).delete();
    }

    @Test
    public void testDelete_WhenK8sReturnsDeploymentWithMatchingId_ResourceDeleted() throws Exception {
        // Arrange.
        String name = UUID.randomUUID().toString();
        String namespace = UUID.randomUUID().toString();
        String uid = UUID.randomUUID().toString();
        Deployment deployment = newDeployment(uid, namespace, name, 1, "image-sample-name");
        mockDeploymentByName(namespace, name, deployment);

        // Act.
        this.deploymentApi.deleteDeployment(uid, namespace, name);

        // Assert.
        verify(this.deploymentMock, times(1)).delete();
    }

    @Test
    public void testUpdateDeploymentReplicaCount_WhenK8ClientThrowsKubernetesClientException_ThrowsVmidcException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcException.class);
        when(this.extensionsApiMock.deployments()).thenThrow(new KubernetesClientException(""));

        // Act.
        this.deploymentApi.updateDeploymentReplicaCount("1234", "sample_name", "sample_label", 1);
    }

    @Test
    public void testUpdateDeploymentReplicaCount_WithReplicaCountLowerThanOne_ThrowsIllegalArgumentException() throws Exception {
        // Arrange.
        this.exception.expect(IllegalArgumentException.class);

        // Act.
        this.deploymentApi.updateDeploymentReplicaCount("1234", "sample_name", "sample_label", 0);
    }

    @Test
    public void testUpdateDeploymentReplicaCount_WhenK8sReturnsNull_ThrowsVmidcException() throws Exception {
        // Arrange.
        String name = UUID.randomUUID().toString();
        String namespace = UUID.randomUUID().toString();
        mockDeploymentByName(namespace, name, null);
        this.exception.expect(VmidcException.class);

        // Act.
        this.deploymentApi.updateDeploymentReplicaCount(UUID.randomUUID().toString(), namespace, name, 1);
    }

    @Test
    public void testUpdateDeploymentReplicaCount_WhenK8sReturnsDeploymentWithMismatchingId_ReturnsNull() throws Exception {
        // Arrange.
        String name = UUID.randomUUID().toString();
        String namespace = UUID.randomUUID().toString();
        mockDeploymentByName(namespace, name, newDeployment(UUID.randomUUID().toString(), name, namespace, 1, "image-sample-name"));
        this.exception.expect(VmidcException.class);

        // Act.
        this.deploymentApi.updateDeploymentReplicaCount(UUID.randomUUID().toString(), namespace, name, 1);
    }

    @Test
    public void testUpdateDeploymentReplicaCount_WhenK8sReturnsDeploymentWithMatchingId_DeploymentUpdated() throws Exception {
        // Arrange.
        String name = UUID.randomUUID().toString();
        String namespace = UUID.randomUUID().toString();
        String uid = UUID.randomUUID().toString();
        Deployment deployment = newDeployment(uid, namespace, name, 1, "image-sample-name");
        ScalableResource<Deployment, DoneableDeployment> resourceMock = mockDeploymentByName(namespace, name, deployment);

        int newReplicaCount = 10;

        // Act.
        this.deploymentApi.updateDeploymentReplicaCount(uid, namespace, name, newReplicaCount);

        // Assert.
        verify(resourceMock, times(1)).scale(newReplicaCount);
    }

    @Test
    public void testCreateDeployment_WithNullDeployment_ThrowsVmidcException() throws Exception {
        // Arrange.
        this.exception.expect(IllegalArgumentException.class);

        // Act.
        this.deploymentApi.createDeployment(null);
    }

    @Test
    public void testCreateDeployment_WhenK8ClientThrowsKubernetesClientException_ThrowsVmidcException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcException.class);
        when(this.extensionsApiMock.deployments()).thenThrow(new KubernetesClientException(""));

        // Act.
        this.deploymentApi.createDeployment(new KubernetesDeployment(null, null, null, 1, null, null));
    }

    @Test
    public void testCreateDeployment_WithValidDeployment_DeploymentCreated() throws Exception {
        // Arrange.
        KubernetesDeployment deployment =
                new KubernetesDeployment("name", "namespace", "uid", 2, "imageName", null);

        String deploymentUid = UUID.randomUUID().toString();

        Deployment createdDeployment = new Deployment();
        ObjectMeta metadata = new ObjectMeta();
        metadata.setUid(deploymentUid);
        createdDeployment.setMetadata(metadata);

        when(
                this.extensionsApiMock
                .deployments()
                .inNamespace(deployment.getNamespace()))
        .thenReturn(this.nonNamespaceOperationMock);

        when(
                this.nonNamespaceOperationMock
                .create(Mockito.argThat(
                        new DeploymentMatcher(
                                deployment.getName(),
                                deployment.getDesiredReplicaCount(),
                                deployment.getContainerImageName()))))
        .thenReturn(createdDeployment);

        // Act.
        String uid = this.deploymentApi.createDeployment(deployment);

        // Assert.
        assertEquals("The returned uid was different than expected.", deploymentUid, uid);
    }

    private ScalableResource<Deployment, DoneableDeployment> mockDeploymentByName(String namespace, String name, Deployment deployment) {
        when(this.extensionsApiMock.deployments().inNamespace(namespace)).thenReturn(this.nonNamespaceOperationMock);

        @SuppressWarnings("unchecked")
        ScalableResource<Deployment, DoneableDeployment> deploymentResource = Mockito.mock(ScalableResource.class);
        when(deploymentResource.get()).thenReturn(deployment);

        when(this.nonNamespaceOperationMock.withName(name)).thenReturn(deploymentResource);
        return deploymentResource;
    }

    private Deployment newDeployment(String uid, String namespace, String name, int desiredReplicaCount, String containerImageName) {
        Deployment deployment = new Deployment();

        ObjectMeta objMeta = new ObjectMeta();
        objMeta.setName(name);
        objMeta.setNamespace(namespace);
        objMeta.setUid(uid);

        DeploymentSpec spec = new DeploymentSpec();
        spec.setReplicas(desiredReplicaCount);

        Container container = new Container();
        container.setImage(containerImageName);

        PodSpec podSpec = new PodSpec();
        podSpec.setContainers(Arrays.asList(container));

        PodTemplateSpec podTemplateSpec = new PodTemplateSpec();
        podTemplateSpec.setSpec(podSpec);
        spec.setTemplate(podTemplateSpec);

        deployment.setMetadata(objMeta);
        deployment.setSpec(spec);

        DeploymentStatus deploymentStatus = new DeploymentStatus();
        deploymentStatus.setAvailableReplicas(3);
        deployment.setStatus(deploymentStatus);

        return deployment;
    }

    private void assertDeploymentFields(Deployment expectedDeployment, KubernetesDeployment actualDeployment) {
        assertEquals("The deployment name was different than the expected.", expectedDeployment.getMetadata().getName(), actualDeployment.getName());
        assertEquals("The deployment namespace was different than the expected.", expectedDeployment.getMetadata().getNamespace(), actualDeployment.getNamespace());
        assertEquals("The deployment uid was different than the expected.", expectedDeployment.getMetadata().getUid(), actualDeployment.getUid());
        assertEquals("The deployment replica count was different than the expected.", (int) expectedDeployment.getSpec().getReplicas(), actualDeployment.getDesiredReplicaCount());
        assertEquals("The deployment available replica count was different than the expected.", (int) expectedDeployment.getStatus().getAvailableReplicas(), actualDeployment.getAvailableReplicaCount());
    }

    private enum DeploymentOperation  {
        GET, DELETE, UPDATE;
    }

    public Object[] getNullInputsTestData() {
        List<Object[]> inputsList = new ArrayList<Object[]>();
        inputsList.add(new Object[]{null, "namespace", "name"});
        inputsList.add(new Object[]{"uuid", null, "name"});
        inputsList.add(new Object[]{"uuid", "namespace", null});

        List<Object[]> result = new ArrayList<Object[]>();

        for (DeploymentOperation operation : DeploymentOperation.values()) {
            for (Object[] inputs : inputsList) {
                result.add(ArrayUtils.add(inputs, operation));
            }
        }

        return result.toArray();
    }

    private class DeploymentMatcher extends ArgumentMatcher<Deployment> {
        private String name;
        private int replicaCount;
        private String imageName;

        public DeploymentMatcher(String name, int replicaCount, String imageName) {
            this.name = name;
            this.replicaCount = replicaCount;
            this.imageName = imageName;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof Deployment)) {
                return false;
            }
            Deployment deployment = (Deployment)object;

            ObjectMeta metadata = deployment.getMetadata();

            DeploymentSpec spec = deployment.getSpec();
            PodTemplateSpec podTemplateSpec = spec.getTemplate();
            String podLabel = podTemplateSpec.getMetadata().getLabels().get("osc-deployment");

            Container container = podTemplateSpec.getSpec().getContainers().get(0);

            return this.name.equals(metadata.getName()) &&
                    this.name.equals(podLabel) &&
                    this.replicaCount == spec.getReplicas() &&
                    this.imageName.equals(container.getImage()) &&
                    this.name.equals(container.getName());
        }
    }
}
