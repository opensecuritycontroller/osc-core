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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.service.exceptions.VmidcException;

import io.fabric8.kubernetes.api.model.ComponentCondition;
import io.fabric8.kubernetes.api.model.ComponentStatus;
import io.fabric8.kubernetes.api.model.ComponentStatusList;
import io.fabric8.kubernetes.api.model.DoneableComponentStatus;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class KubernetesStatusApiTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private org.osc.core.broker.rest.client.k8s.KubernetesClient kubernetesClient;

    @Mock
    private DefaultKubernetesClient fabric8Client;

    @Mock
    private MixedOperation<ComponentStatus, ComponentStatusList, DoneableComponentStatus, Resource<ComponentStatus,DoneableComponentStatus>> mixedOperationMock;

    private KubernetesStatusApi api;

    @Before
    public void testInitialize() throws Exception{
        MockitoAnnotations.initMocks(this);
        when(this.kubernetesClient.getClient()).thenReturn(this.fabric8Client);
        this.api = new KubernetesStatusApi(this.kubernetesClient);

        when(this.fabric8Client.componentstatuses()).thenReturn(this.mixedOperationMock);
    }

    @Test
    public void testIsServiceReady_WhenK8sClientThrowsKubernetesClientException_ThrowsVmidcException() throws Exception {
        // Arrange.
        this.exception.expect(VmidcException.class);
        when(this.fabric8Client.componentstatuses()).thenThrow(new KubernetesClientException(""));

        // Act.
        this.api.isServiceReady();
    }

    @Test
    public void testIsServiceReady_WhenComponentStatusIsNull_ServiceNotReady() throws Exception {
        // Arrange.
        mockComponentStatus(null);

        // Act.
        boolean isServiceReady = this.api.isServiceReady();

        // Assert.
        assertFalse(isServiceReady);
    }

    @Test
    public void testIsServiceReady_WhenComponentStatusConditionsIsNull_ServiceNotReady() throws Exception {
        // Arrange.
        ComponentStatus componentStatus = newComponentStatus(null, null);
        componentStatus.setConditions(null);
        mockComponentStatus(componentStatus);

        // Act.
        boolean isServiceReady = this.api.isServiceReady();

        // Assert.
        assertFalse(isServiceReady);
    }


    @Test
    public void testIsServiceReady_WhenComponentStatusHealthyConditionNotFound_ServiceNotReady() throws Exception {
        // Arrange.
        ComponentStatus componentStatus = newComponentStatus("other_condition", "True");
        mockComponentStatus(componentStatus);

        // Act.
        boolean isServiceReady = this.api.isServiceReady();

        // Assert.
        assertFalse(isServiceReady);
    }


    @Test
    public void testIsServiceReady_WhenComponentStatusConditionFalse_ServiceNotReady() throws Exception {
        // Arrange.
        ComponentStatus componentStatus = newComponentStatus("Healthy", "False");
        mockComponentStatus(componentStatus);

        // Act.
        boolean isServiceReady = this.api.isServiceReady();

        // Assert.
        assertFalse(isServiceReady);
    }

    @Test
    public void testIsServiceReady_WhenComponentStatusConditionFalse_ServiceReady() throws Exception {
        // Arrange.
        ComponentStatus componentStatus = newComponentStatus("Healthy", "True");
        mockComponentStatus(componentStatus);

        // Act.
        boolean isServiceReady = this.api.isServiceReady();

        // Assert.
        assertTrue(isServiceReady);
    }

    private void mockComponentStatus(ComponentStatus result) {
        @SuppressWarnings("unchecked")
        Resource<ComponentStatus, DoneableComponentStatus> componentStatusResource = Mockito.mock(Resource.class);
        when(componentStatusResource.get()).thenReturn(result);
        when(this.mixedOperationMock.withName("controller-manager")).thenReturn(componentStatusResource);
    }

    private ComponentStatus newComponentStatus(String conditionType, String conditionStatus) {
        ComponentStatus componentStatus = new ComponentStatus();
        List<ComponentCondition> conditions = new ArrayList<ComponentCondition>();

        if (conditionType != null) {
            ComponentCondition condition = new ComponentCondition();
            condition.setType(conditionType);
            condition.setStatus(conditionStatus);
            conditions.add(condition);
        }

        componentStatus.setConditions(conditions);
        return componentStatus;
    }
}
