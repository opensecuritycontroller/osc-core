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
import org.osc.core.broker.model.entities.virtualization.k8s.Label;
import org.osc.core.broker.model.entities.virtualization.k8s.Pod;
import org.osc.core.broker.model.entities.virtualization.k8s.PodPort;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;

public class DeleteK8sLabelPodTaskTest  {
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

    @InjectMocks
    DeleteK8sLabelPodTask factory;

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(this.em.getTransaction()).thenReturn(this.tx);

        this.txControl.setEntityManager(this.em);

        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);
    }

    @Test
    public void testExecute_WithPodAssignedToAnotherMember_NothingIsRemoved() throws Exception {
        // Arrange.
        Pod pod = createAndRegisterPod(100L);
        pod.getLabels().add(new Label(UUID.randomUUID().toString(), UUID.randomUUID().toString()));

        Label memberLabel = new Label(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        DeleteK8sLabelPodTask task = this.factory.create(pod, memberLabel);

        // Act.
        task.execute();

        // Assert.
        verify(this.em, Mockito.never()).remove(Mockito.any(PodPort.class));
        verify(this.em, Mockito.never()).remove(Mockito.any(Pod.class));
    }

    @Test
    public void testExecute_WithPodAssignedToTargetedMember_PodAndPortRemoved() throws Exception {
        // Arrange.
        Pod pod = createAndRegisterPod(100L);
        String sameLabel = UUID.randomUUID().toString();
        pod.getLabels().add(new Label(UUID.randomUUID().toString(), sameLabel));

        Label memberLabel = new Label(UUID.randomUUID().toString(), sameLabel);
        DeleteK8sLabelPodTask task = this.factory.create(pod, memberLabel);

        // Act.
        task.execute();

        // Assert.
        verify(this.em, Mockito.times(1)).remove(pod.getPorts().iterator().next());
        verify(this.em, Mockito.times(1)).remove(pod);
    }

    private Pod createAndRegisterPod(long id) {
        Pod pod = new Pod(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString());

        pod.setId(id);
        pod.getPorts().add(new PodPort(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()));
        when(this.em.find(Pod.class, pod.getId())).thenReturn(pod);

        return pod;
    }
}
