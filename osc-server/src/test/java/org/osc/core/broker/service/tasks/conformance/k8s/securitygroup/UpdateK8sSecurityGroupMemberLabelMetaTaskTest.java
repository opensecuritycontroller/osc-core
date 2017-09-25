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
package org.osc.core.broker.service.tasks.conformance.k8s.securitygroup;

import static org.osc.core.broker.service.tasks.conformance.k8s.securitygroup.UpdateK8sSecurityGroupMemberLabelMetaTaskTestData.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.k8s.Label;
import org.osc.core.broker.rest.client.k8s.KubernetesPod;
import org.osc.core.broker.rest.client.k8s.KubernetesPodApi;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TaskGraphHelper;
import org.osc.core.test.util.TestTransactionControl;

@RunWith(Parameterized.class)
public class UpdateK8sSecurityGroupMemberLabelMetaTaskTest {

    public EntityManager em;

    private SecurityGroupMember sgm;
    private TaskGraph expectedGraph;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TestTransactionControl txControl;

    @Mock
    DBConnectionManager dbMgr;

    @Mock
    TransactionalBroadcastUtil txBroadcastUtil;

    @InjectMocks
    UpdateK8sSecurityGroupMemberLabelMetaTask factoryTask;

    @Mock
    private KubernetesPodApi k8sPodApi;

    public UpdateK8sSecurityGroupMemberLabelMetaTaskTest(SecurityGroupMember sgm, TaskGraph expectedGraph) {
        this.sgm = sgm;
        this.expectedGraph = expectedGraph;
    }

    @Before
    public void testInitialize() throws VmidcException {
        MockitoAnnotations.initMocks(this);
        this.em = InMemDB.getEntityManagerFactory().createEntityManager();

        this.txControl.setEntityManager(this.em);
        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);
        registerKubernetesPods(this.sgm.getLabel(), MATCHING_PODS);
        persist(this.sgm, this.em);
    }

    @After
    public void testTearDowm() {
        InMemDB.shutdown();
    }

    @Test
    public void testExecute_WithVariousSGM_ExpectCorrectTaskGraph() throws Exception {
        // Arrange.
        UpdateK8sSecurityGroupMemberLabelMetaTask task = this.factoryTask.create(this.sgm, this.k8sPodApi);
        task.labelPodCreateTask = new LabelPodCreateTask();
        task.labelPodDeleteTask = new LabelPodDeleteTask();

        // Act.
        task.execute();

        // Assert.
        TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
    }

    @Parameters()
    public static Collection<Object[]> getTestData() {
        return Arrays.asList(new Object[][] {
            { NO_ENTITY_ORPHAN_PODS_SGM, createExpectedGraph(NO_ENTITY_ORPHAN_PODS_SGM) },
            { ORPHAN_ENTITIES_NO_PODS_SGM, createExpectedGraph(ORPHAN_ENTITIES_NO_PODS_SGM) },
            { ENTITIES_PODS_MATCHING_SGM, createExpectedGraph(ENTITIES_PODS_MATCHING_SGM) },
            { SOME_ORPHAN_ENTITIES_SOME_ORPHAN_PODS_SGM, createExpectedGraph(SOME_ORPHAN_ENTITIES_SOME_ORPHAN_PODS_SGM) }
        });
    }

    private void registerKubernetesPods(Label label, List<KubernetesPod> k8sPods) throws VmidcException {
        Mockito
        .when(this.k8sPodApi.getPodsByLabel(label.getValue()))
        .thenReturn(k8sPods);
        this.k8sPodApi.getPodsByLabel(label.getValue());
    }
}