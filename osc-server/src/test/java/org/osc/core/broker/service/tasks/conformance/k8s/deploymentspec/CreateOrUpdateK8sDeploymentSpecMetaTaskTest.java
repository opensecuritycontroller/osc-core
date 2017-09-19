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

import static org.osc.core.broker.service.tasks.conformance.k8s.deploymentspec.CreateOrUpdateK8sDeploymentSpecMetaTaskTestData.*;

import java.util.Arrays;
import java.util.Collection;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.k8s.KubernetesDeployment;
import org.osc.core.broker.rest.client.k8s.KubernetesDeploymentApi;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TaskGraphHelper;
import org.osc.core.test.util.TestTransactionControl;

@RunWith(Parameterized.class)
public class CreateOrUpdateK8sDeploymentSpecMetaTaskTest {
    public EntityManager em;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TestTransactionControl txControl;

    @Mock
    DBConnectionManager dbMgr;

    @Mock
    TransactionalBroadcastUtil txBroadcastUtil;

    @InjectMocks
    CreateOrUpdateK8sDeploymentSpecMetaTask factoryTask;

    @Mock
    private KubernetesDeploymentApi k8sDeploymentApi;

    private DeploymentSpec ds;

    private TaskGraph expectedGraph;

    public CreateOrUpdateK8sDeploymentSpecMetaTaskTest(DeploymentSpec ds, TaskGraph tg) {
        this.ds = ds;
        this.expectedGraph = tg;
    }

    @Before
    public void testInitialize() throws Exception{
        MockitoAnnotations.initMocks(this);

        this.em = InMemDB.getEntityManagerFactory().createEntityManager();

        this.txControl.setEntityManager(this.em);

        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);

        populateDatabase();

        registerKubernetesDeployment(CREATE_DS_NO_K8S_DEPLOYMENT, null);

        KubernetesDeployment k8sDeployment = new KubernetesDeployment("name", "namespace", "uid", UPDATE_DS_NEW_INSTANCE_COUNT.getInstanceCount() + 1, "containerImageName", null);
        registerKubernetesDeployment(UPDATE_DS_NEW_INSTANCE_COUNT, k8sDeployment);

        KubernetesDeployment k8sUpToDateDeployment = new KubernetesDeployment("name", "namespace", "uid", UPDATE_DS_NEW_INSTANCE_COUNT.getInstanceCount(), "containerImageName", null);
        registerKubernetesDeployment(NOOP_DS_SAME_INSTANCE_COUNT, k8sUpToDateDeployment);
    }

    @After
    public void testTearDown() {
        InMemDB.shutdown();
    }

    private void populateDatabase() {
        this.em.getTransaction().begin();

        this.em.persist(this.ds.getVirtualSystem()
                .getVirtualizationConnector());
        this.em.persist(this.ds.getVirtualSystem()
                .getDistributedAppliance().getApplianceManagerConnector());
        this.em.persist(this.ds.getVirtualSystem()
                .getDistributedAppliance().getAppliance());
        this.em.persist(this.ds.getVirtualSystem().getDistributedAppliance());
        this.em.persist(this.ds.getVirtualSystem().getApplianceSoftwareVersion());
        this.em.persist(this.ds.getVirtualSystem().getDomain());
        this.em.persist(this.ds.getVirtualSystem());
        for(DistributedApplianceInstance dai : this.ds.getVirtualSystem().getDistributedApplianceInstances()) {
            this.em.persist(dai);
        }

        this.em.persist(this.ds);

        this.em.getTransaction().commit();
    }

    @Test
    public void testExecuteTransaction_WithVariousDeploymentSpecs_ExpectsCorrectTaskGraph() throws Exception {
        // Arrange.
        this.factoryTask.createK8sDeploymentTask = new CreateK8sDeploymentTask();
        this.factoryTask.updateK8sDeploymentTask = new UpdateK8sDeploymentTask();
        this.factoryTask.checkK8sDeploymentStateTask = new CheckK8sDeploymentStateTask();
        this.factoryTask.conformK8sDeploymentPodsMetaTask = new ConformK8sDeploymentPodsMetaTask();

        CreateOrUpdateK8sDeploymentSpecMetaTask task = this.factoryTask.create(this.ds, this.k8sDeploymentApi);

        // Act.
        task.execute();

        // Assert.
        TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
    }

    @Parameters()
    public static Collection<Object[]> getTestData() {
        return Arrays.asList(new Object[][] {
            {CREATE_DS_NO_EXTERNAL_ID, createDSGraph(CREATE_DS_NO_EXTERNAL_ID)},
            {CREATE_DS_NO_K8S_DEPLOYMENT, createDSGraph(CREATE_DS_NO_K8S_DEPLOYMENT)},
            {UPDATE_DS_NEW_INSTANCE_COUNT, updateDSGraph(UPDATE_DS_NEW_INSTANCE_COUNT)},
            {NOOP_DS_SAME_INSTANCE_COUNT, emptyDSGraph()},
        });
    }

    private void registerKubernetesDeployment(DeploymentSpec ds, KubernetesDeployment k8sDeployment) throws VmidcException {
        Mockito.when(this.k8sDeploymentApi
                .getDeploymentById(
                        ds.getExternalId(),
                        ds.getNamespace(),
                        K8sUtil.getK8sName(ds))).thenReturn(k8sDeployment);
    }
}
