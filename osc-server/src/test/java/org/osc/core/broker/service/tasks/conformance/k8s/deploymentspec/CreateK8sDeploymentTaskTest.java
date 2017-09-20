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

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.k8s.KubernetesDeployment;
import org.osc.core.broker.rest.client.k8s.KubernetesDeploymentApi;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;

public class CreateK8sDeploymentTaskTest  {
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
    CreateK8sDeploymentTask factory;

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(this.em.getTransaction()).thenReturn(this.tx);

        this.txControl.setEntityManager(this.em);

        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);
    }

    @Test
    public void testExecute_WhenCreateDeploymentSucceeds_DSIsUpdatedWithK8sDeploymentID() throws Exception {
        // Arrange.
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
        ds.setName("ds-name");
        ds.setNamespace("ds-namespace");
        ds.setInstanceCount(5);

        when(this.em.find(DeploymentSpec.class, ds.getId())).thenReturn(ds);

        String k8sDeploymentId = UUID.randomUUID().toString();

        mockCreateK8sDeployment(ds, k8sDeploymentId);

        CreateK8sDeploymentTask task = this.factory.create(ds, this.k8sDeploymentApi);

        // Act.
        task.execute();

        // Assert.
        Assert.assertEquals("The deployment spec external id was different than expected", k8sDeploymentId, ds.getExternalId());
        verify(this.em, Mockito.times(1)).merge(ds);
    }

    private void mockCreateK8sDeployment(DeploymentSpec ds, String k8sDeploymentId) throws Exception {
        when(this.k8sDeploymentApi.createDeployment(argThat(new KubernetesDeploymentMatcher(CreateK8sDeploymentTask.getK8sName(ds),
                ds.getNamespace(),
                ds.getInstanceCount(),
                ds.getVirtualSystem().getApplianceSoftwareVersion().getImageUrl(),
                ds.getVirtualSystem().getApplianceSoftwareVersion().getImagePullSecretName()))))
        .thenReturn(k8sDeploymentId);
    }

    private class KubernetesDeploymentMatcher extends ArgumentMatcher<KubernetesDeployment> {
        private String name;
        private String namespace;
        private int instanceCount;
        private String imageUrl;
        private String imagePullSecretName;

        public KubernetesDeploymentMatcher(String name, String namespace, int instanceCount, String imageUrl, String imagePullSecretName) {
            this.name = name;
            this.namespace = namespace;
            this.instanceCount = instanceCount;
            this.imageUrl = imageUrl;
            this.imagePullSecretName = imagePullSecretName;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof KubernetesDeployment)) {
                return false;
            }

            KubernetesDeployment k8sDeployment = (KubernetesDeployment) object;
            return this.name.equals(k8sDeployment.getName()) &&
                    this.namespace.equals(k8sDeployment.getNamespace()) &&
                    this.instanceCount == k8sDeployment.getDesiredReplicaCount() &&
                    this.imageUrl.equals(k8sDeployment.getContainerImageName()) &&
                    this.imagePullSecretName.equals(k8sDeployment.getImagePullSecretName());
        }
    }
}
