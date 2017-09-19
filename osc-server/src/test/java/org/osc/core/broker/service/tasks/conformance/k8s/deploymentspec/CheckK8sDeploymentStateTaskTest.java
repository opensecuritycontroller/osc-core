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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.k8s.KubernetesDeployment;
import org.osc.core.broker.rest.client.k8s.KubernetesDeploymentApi;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;

public class CheckK8sDeploymentStateTaskTest  {
    @Mock
    protected EntityManager em;

    @Mock
    protected EntityTransaction tx;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    TestTransactionControl txControl;

    @Mock
    DBConnectionManager dbMgr;

    @Mock
    TransactionalBroadcastUtil txBroadcastUtil;

    @Mock
    private KubernetesDeploymentApi k8sDeploymentApi;

    @InjectMocks
    CheckK8sDeploymentStateTask factory;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(this.em.getTransaction()).thenReturn(this.tx);

        this.txControl.setEntityManager(this.em);

        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);
    }

    @Test
    public void testExecute_WhenGetDeploymentReturnsNull_ThrowsVmidcException() throws Exception {
        // Arrange.
        DeploymentSpec ds = createDS();
        when(this.em.find(DeploymentSpec.class, ds.getId())).thenReturn(ds);
        registerKubernetesDeployment(ds, null);

        CheckK8sDeploymentStateTask task = this.factory.create(ds, this.k8sDeploymentApi);
        this.exception.expect(VmidcException.class);
        this.exception.expectMessage("Kubernetes returned a null deployment");

        // Act.
        task.execute();
    }

    @Test
    public void testExecute_WhenGetDeploymentNeverReturnsExpectedInstanceCount_ThrowsVmidcException() throws Exception {
        // Arrange.
        DeploymentSpec ds = createDS();
        when(this.em.find(DeploymentSpec.class, ds.getId())).thenReturn(ds);

        KubernetesDeployment k8sDeployment = Mockito.mock(KubernetesDeployment.class);
        when(k8sDeployment.getAvailableReplicaCount()).thenReturn(ds.getInstanceCount() - 1);

        registerKubernetesDeployment(ds, k8sDeployment);
        int numberOfRetries = 4;
        int retryInterval = 1;

        CheckK8sDeploymentStateTask task = this.factory.create(ds, this.k8sDeploymentApi);
        task.MAX_RETRIES = numberOfRetries;
        task.RETRY_INTERVAL__MILLISECONDS = retryInterval;
        this.exception.expect(VmidcException.class);
        this.exception.expectMessage("The Kubernetes deployment failed to reach the desired replica count");

        // Act.
        task.execute();

        // Assert.
        verify(this.k8sDeploymentApi.getDeploymentById(anyString(), anyString(), anyString()), times(numberOfRetries));
    }

    @Test
    public void testExecute_WhenGetDeploymentReturnsExpectedCount_SucceedsInFirstAttempt() throws Exception {
        // Arrange.
        DeploymentSpec ds = createDS();
        when(this.em.find(DeploymentSpec.class, ds.getId())).thenReturn(ds);

        KubernetesDeployment k8sDeployment = Mockito.mock(KubernetesDeployment.class);
        when(k8sDeployment.getAvailableReplicaCount()).thenReturn(ds.getInstanceCount());

        registerKubernetesDeployment(ds, k8sDeployment);

        CheckK8sDeploymentStateTask task = this.factory.create(ds, this.k8sDeploymentApi);

        // Act.
        task.execute();

        // Assert.
        verify(this.k8sDeploymentApi, times(1)).getDeploymentById(anyString(), anyString(), anyString());
    }

    @Test
    public void testExecute_WhenGetDeploymentReturnsExpectedLastAttempt_SucceedsInLastAttempt() throws Exception {
        // Arrange.
        DeploymentSpec ds = createDS();
        when(this.em.find(DeploymentSpec.class, ds.getId())).thenReturn(ds);
        int numberOfRetries = 4;
        int retryInterval = 1;

        registerAvailableKubernetesDeploymentAfterCount(ds, numberOfRetries - 1);

        CheckK8sDeploymentStateTask task = this.factory.create(ds, this.k8sDeploymentApi);
        task.MAX_RETRIES = numberOfRetries;
        task.RETRY_INTERVAL__MILLISECONDS = retryInterval;

        // Act.
        task.execute();

        // Assert.
        verify(this.k8sDeploymentApi, times(numberOfRetries)).getDeploymentById(anyString(), anyString(), anyString());
    }

    private void registerKubernetesDeployment(DeploymentSpec ds, KubernetesDeployment k8sDeployment) throws VmidcException {
        when(this.k8sDeploymentApi
                .getDeploymentById(
                        ds.getExternalId(),
                        ds.getNamespace(),
                        K8sUtil.getK8sName(ds))).thenReturn(k8sDeployment);
    }

    private void registerAvailableKubernetesDeploymentAfterCount(DeploymentSpec ds, int count) throws VmidcException {
        KubernetesDeployment unavailableK8sDeployment = Mockito.mock(KubernetesDeployment.class);
        when(unavailableK8sDeployment.getAvailableReplicaCount())
        .thenReturn(ds.getInstanceCount() - 1);

        KubernetesDeployment availableK8sDeployment = Mockito.mock(KubernetesDeployment.class);
        when(availableK8sDeployment.getAvailableReplicaCount())
        .thenReturn(ds.getInstanceCount());

        when(this.k8sDeploymentApi
                .getDeploymentById(
                        ds.getExternalId(),
                        ds.getNamespace(),
                        K8sUtil.getK8sName(ds))).thenAnswer(new Answer<KubernetesDeployment>() {
                            private int i = 0;

                            @Override
                            public KubernetesDeployment answer(InvocationOnMock invocation) {
                                if (this.i++ == count) {
                                    return availableK8sDeployment;
                                } else {
                                    return unavailableK8sDeployment;
                                }
                            }
                        });;
    }

    private DeploymentSpec createDS() {
        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setProviderIpAddress("1.1.1.1");
        VirtualSystem vs = new VirtualSystem(null);
        vs.setVirtualizationConnector(vc);
        vs.setId(102L);

        ApplianceSoftwareVersion avs = new ApplianceSoftwareVersion();
        avs.setImageUrl("ds-image-url");
        avs.setImagePullSecretName("ds-pull-secret-name");

        vs.setApplianceSoftwareVersion(avs);
        DeploymentSpec ds = new DeploymentSpec(vs, null, null, null, null, null);
        ds.setId(101L);
        ds.setName(UUID.randomUUID().toString());
        ds.setNamespace(UUID.randomUUID().toString());
        ds.setExternalId(UUID.randomUUID().toString());
        ds.setInstanceCount(5);

        return ds;
    }
}
