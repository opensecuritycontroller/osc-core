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

import static org.junit.Assert.assertEquals;
import static org.osc.core.broker.service.tasks.conformance.k8s.securitygroup.LabelPodCreateTaskTestData.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.k8s.Label;
import org.osc.core.broker.model.entities.virtualization.k8s.Pod;
import org.osc.core.broker.rest.client.k8s.KubernetesPod;
import org.osc.core.broker.rest.client.k8s.KubernetesPodApi;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;

@RunWith(Parameterized.class)
public class LabelPodCreateTaskTest {

    public EntityManager em;

    private SecurityGroupMember unrelatedSgm;
    private KubernetesPod kubernetesPod;
    private Label labelUnderTest;
    private Label unrelatedLabelEntity;
    private String otherUnknownLabelValue;

    @Rule
    public ExpectedException exception = ExpectedException.none();
    public Class<? extends Exception> expectedExceptionClass;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TestTransactionControl txControl;

    @Mock
    DBConnectionManager dbMgr;

    @Mock
    TransactionalBroadcastUtil txBroadcastUtil;

    @InjectMocks
    LabelPodCreateTask factoryTask;

    @Mock
    private KubernetesPodApi k8sPodApi;

    public LabelPodCreateTaskTest(KubernetesPod kubernetesPod, SecurityGroupMember unrelatedSgm, Label unrelatedLabelEntity,
            String otherUnknownLabel, Class<? extends Exception> expectedException) {
        this.kubernetesPod = kubernetesPod;
        this.labelUnderTest = createLabelUnderTest();
        this.unrelatedSgm = unrelatedSgm;
        this.unrelatedLabelEntity = unrelatedLabelEntity;

        this.otherUnknownLabelValue = otherUnknownLabel;
        this.expectedExceptionClass = expectedException;
    }

    @Before
    public void testInitialize() throws VmidcException {
        MockitoAnnotations.initMocks(this);
        this.em = InMemDB.getEntityManagerFactory().createEntityManager();

        this.txControl.setEntityManager(this.em);
        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);

        persist(this.labelUnderTest, this.em);
        if (this.expectedExceptionClass != null) {
            this.exception.expect(this.expectedExceptionClass);
        }
        if (this.unrelatedSgm != null) {
            registerKubernetesPods(this.unrelatedSgm.getLabel(), this.kubernetesPod);
            persist(this.unrelatedSgm, this.em);
        } else if (this.unrelatedLabelEntity != null) {
            registerKubernetesPods(this.unrelatedLabelEntity, this.kubernetesPod);
            persist(this.unrelatedLabelEntity, this.em);
        } else if (this.otherUnknownLabelValue != null) {
            registerKubernetesPods(this.otherUnknownLabelValue, this.kubernetesPod);
        }
    }

    @After
    public void testTearDowm() {
        InMemDB.shutdown();
    }

    @Test
    public void testDummy() {

    }

//    @Test
    public void testExecute_WithVariousK8sPods_ExpectPodEntityUnderSGUnlessAlreadyProtected() throws Exception {
        // Arrange.
        LabelPodCreateTask task = this.factoryTask.create(this.kubernetesPod, this.labelUnderTest, this.k8sPodApi);

        // Act.
        task.execute();

        // Assert.
        Pod resultPod = this.txControl.required(() -> {
            CriteriaBuilder cb = this.em.getCriteriaBuilder();
            CriteriaQuery<Pod> criteria = cb.createQuery(Pod.class);
            Root<Pod> root = criteria.from(Pod.class);

            criteria.select(root).where(cb.equal(root.get("externalId"), this.kubernetesPod.getUid()));

            List<Pod> results = this.em.createQuery(criteria).getResultList();

            assertEquals(1, results.size());
            return results.get(0);
        });


        this.txControl.required(() -> {
            CriteriaBuilder cb = this.em.getCriteriaBuilder();
            CriteriaQuery<Label> criteria = cb.createQuery(Label.class);
            Root<Label> root = criteria.from(Label.class);

            criteria.select(root).where(cb.equal(root.get("id"), this.labelUnderTest.getId()));

            List<Label> results = this.em.createQuery(criteria).getResultList();

            assertEquals(1, results.size());

            Label resultLabel = results.get(0);
            List<Pod> labelPods = resultLabel.getPods().stream()
                .filter(p -> p.getExternalId().equals(this.kubernetesPod.getUid())).collect(Collectors.toList());

            assertEquals(1, labelPods.size());
            Assert.assertEquals(resultPod.getId(), labelPods.get(0).getId());
            Assert.assertEquals(this.kubernetesPod.getUid(), labelPods.get(0).getExternalId());
            return results.get(0);
        });
    }

    @Parameters()
    public static Collection<Object[]> getTestData() {
        return Arrays.asList(new Object[][] {
            { NEW_UNKNOWN_POD_NO_OTHER_LABEL, null, null, null, null },
            { NEW_UNKNOWN_POD_WITH_OTHER_UNPROTECTED_LABEL, null, null, OTHER_UNKNOWN_LABEL_VALUE, null },
            { NEW_KNOWN_POD_WITH_OTHER_UNPROTECTED_LABEL, null, OTHER_LABEL_WITH_KNOWN_POD_ENTITY, null, null },
//            { NEW_KNOWN_POD_WITH_OTHER_PROTECTED_LABEL, OTHEL_LABEL_SGM, null, null, VmidcException.class},
        });
    }

    private void registerKubernetesPods(Label label, KubernetesPod kubernetesPod) throws VmidcException {
        registerKubernetesPods(label.getValue(), kubernetesPod);
    }
    private void registerKubernetesPods(String labelValue, KubernetesPod kubernetesPod) throws VmidcException {
        Mockito
        .when(this.k8sPodApi.getPodsByLabel(labelValue))
        .thenReturn(Arrays.asList(kubernetesPod));
    }
}
