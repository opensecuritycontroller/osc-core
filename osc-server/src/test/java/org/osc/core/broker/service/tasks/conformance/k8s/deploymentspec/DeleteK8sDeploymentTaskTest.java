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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.k8s.KubernetesDeploymentApi;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;

public class DeleteK8sDeploymentTaskTest  {
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
    DeleteK8sDeploymentTask factory;

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(this.em.getTransaction()).thenReturn(this.tx);

        this.txControl.setEntityManager(this.em);

        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);
    }

    @Test
    public void testExecute_WithExistingDS_K8sDSIsDeleted() throws Exception {
        // Arrange.
        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setProviderIpAddress("1.1.1.1");
        VirtualSystem vs = new VirtualSystem(null);
        vs.setVirtualizationConnector(vc);
        vs.setId(102L);

        ApplianceSoftwareVersion asv = new ApplianceSoftwareVersion();
        asv.setImageUrl("ds-image-url");
        asv.setImagePullSecretName("ds-pull-secret-name");

        vs.setApplianceSoftwareVersion(asv);
        DeploymentSpec ds = new DeploymentSpec(vs, null, null, null, null, null);
        ds.setId(101L);
        ds.setName("ds-name");
        ds.setNamespace("ds-namespace");
        ds.setInstanceCount(8);
        ds.setExternalId(UUID.randomUUID().toString());

        when(this.em.find(DeploymentSpec.class, ds.getId())).thenReturn(ds);

        DeleteK8sDeploymentTask task = this.factory.create(ds, this.k8sDeploymentApi);

        // Act.
        task.execute();

        // Assert.
        verify(this.k8sDeploymentApi, Mockito.times(1)).deleteDeployment(
                ds.getExternalId(),
                ds.getNamespace(),
                K8sUtil.getK8sName(ds));
    }
}
