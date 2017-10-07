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
package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.k8s.Label;
import org.osc.core.broker.model.entities.virtualization.k8s.Pod;
import org.osc.core.broker.model.entities.virtualization.k8s.PodPort;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;

public class SecurityGroupMemberDeleteTaskTest {
    @Mock
    protected EntityManager em;
    @Mock
    protected EntityTransaction tx;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    TestTransactionControl txControl;

    @Mock
    public ApiFactoryService apiFactoryServiceMock;

    @Mock
    DBConnectionManager dbMgr;

    @Mock
    TransactionalBroadcastUtil txBroadcastUtil;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @InjectMocks
    SecurityGroupMemberDeleteTask factoryTask;

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(this.em.getTransaction()).thenReturn(this.tx);

        this.txControl.setEntityManager(this.em);

        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);
    }

    @Test
    public void testExecute_WhenLabelHasMultipleSGMs_LabelIsNotDelete()
            throws Exception {
        // Arrange.
        Label label = new Label("labelName", "labelValue");
        SecurityGroup sg = new SecurityGroup(null, null, null);
        sg.setName("sgName");

        label.getSecurityGroupMembers().add(new SecurityGroupMember(sg, new Label(null, null)));

        SecurityGroupMember sgm = new SecurityGroupMember(sg, label);
        label.getSecurityGroupMembers().add(sgm);

        sgm.setId(100L);
        when(this.em.find(SecurityGroupMember.class, sgm.getId())).thenReturn(sgm);

        SecurityGroupMemberDeleteTask task = this.factoryTask.create(sgm);

        // Act.
        task.execute();

        // Assert.
        verify(this.em, times(1)).remove(sgm);
        verify(this.em, never()).remove(isA(Label.class));
        verify(this.em, never()).remove(isA(Pod.class));
        verify(this.em, never()).remove(isA(PodPort.class));
    }

    @Test
    public void testExecute_WhenLabelHasSingleSGM_PodAndPodPortAreNotDeleted()
            throws Exception {
        // Arrange.
        Label label = new Label("labelName", "labelValue");
        SecurityGroup sg = new SecurityGroup(null, null, null);
        sg.setName("sgName");

        SecurityGroupMember sgm = new SecurityGroupMember(sg, label);
        label.getSecurityGroupMembers().add(sgm);

        sgm.setId(100L);
        when(this.em.find(SecurityGroupMember.class, sgm.getId())).thenReturn(sgm);

        SecurityGroupMemberDeleteTask task = this.factoryTask.create(sgm);

        // Act.
        task.execute();

        // Assert.
        verify(this.em, times(1)).remove(sgm);
        verify(this.em, times(1)).remove(isA(Label.class));
        verify(this.em, never()).remove(isA(Pod.class));
        verify(this.em, never()).remove(isA(PodPort.class));
    }

    @Test
    public void testExecute_WhenLabelHasExclusivePod_PodAndPodPortAreDeleted()
            throws Exception {
        // Arrange.
        Label label = new Label("labelName", "labelValue");
        Pod pod = new Pod("name", "namespace", "node", "externalId");
        pod.setId(101L);
        pod.getLabels().add(label);
        PodPort podPort = new PodPort("externalId", "macAddress", "ipAddress", "parentId");
        podPort.setId(102L);
        pod.getPorts().add(podPort);

        label.getPods().add(pod);
        SecurityGroup sg = new SecurityGroup(null, null, null);
        sg.setName("sgName");

        SecurityGroupMember sgm = new SecurityGroupMember(sg, label);
        label.getSecurityGroupMembers().add(sgm);

        sgm.setId(100L);
        when(this.em.find(SecurityGroupMember.class, sgm.getId())).thenReturn(sgm);

        SecurityGroupMemberDeleteTask task = this.factoryTask.create(sgm);

        // Act.
        task.execute();

        // Assert.
        verify(this.em, times(1)).remove(sgm);
        verify(this.em, times(1)).remove(label);
        verify(this.em, times(1)).remove(pod);
        verify(this.em, times(1)).remove(podPort);
    }

    @Test
    public void testExecute_WhenLabelHasExclusivePod_WhenPodPortHasAssignedDAI_ProtectedPodPortIsUnassignedFromDAI()
            throws Exception {
        // Arrange.
        Label label = new Label("labelName", "labelValue");
        Pod pod = new Pod("name", "namespace", "node", "externalId");
        pod.setId(101L);
        pod.getLabels().add(label);
        PodPort podPort = new PodPort("externalId", "macAddress", "ipAddress", "parentId");
        podPort.setId(102L);
        pod.getPorts().add(podPort);

        DistributedApplianceInstance dai = new DistributedApplianceInstance();
        dai.addProtectedPort(podPort);
        dai.setId(200L);
        podPort.getDais().add(dai);

        label.getPods().add(pod);
        SecurityGroup sg = new SecurityGroup(null, null, null);
        sg.setName("sgName");

        SecurityGroupMember sgm = new SecurityGroupMember(sg, label);
        label.getSecurityGroupMembers().add(sgm);

        sgm.setId(100L);
        when(this.em.find(SecurityGroupMember.class, sgm.getId())).thenReturn(sgm);

        SecurityGroupMemberDeleteTask task = this.factoryTask.create(sgm);

        // Act.
        task.execute();

        // Assert.
        verify(this.em, times(1)).remove(sgm);
        verify(this.em, times(1)).remove(label);
        verify(this.em, times(1)).remove(pod);
        verify(this.em, times(1)).remove(podPort);
        verify(this.em, times(1)).merge(dai);
        Assert.assertTrue("The DAI should not have any protected port.", dai.getProtectedPorts().isEmpty());
    }

    @Test
    public void testExecute_WhenLabelHasSharedPod_PodAndPodPortAreNotDeleted()
            throws Exception {
        // Arrange.
        Label label = new Label("labelName", "labelValue");
        Pod pod = new Pod("name", "namespace", "node", "externalId");
        pod.setId(101L);
        PodPort podPort = new PodPort("externalId", "macAddress", "ipAddress", "parentId");
        podPort.setId(102L);
        pod.getPorts().add(podPort);

        label.getPods().add(pod);
        label.getPods().add(new Pod("name", "namespace", "node", "externalId"));

        SecurityGroup sg = new SecurityGroup(null, null, null);
        sg.setName("sgName");

        SecurityGroupMember sgm = new SecurityGroupMember(sg, label);
        label.getSecurityGroupMembers().add(sgm);

        sgm.setId(100L);
        when(this.em.find(SecurityGroupMember.class, sgm.getId())).thenReturn(sgm);

        SecurityGroupMemberDeleteTask task = this.factoryTask.create(sgm);

        // Act.
        task.execute();

        // Assert.
        verify(this.em, times(1)).remove(sgm);
        verify(this.em, times(1)).remove(label);
        verify(this.em, never()).remove(isA(Pod.class));
        verify(this.em, never()).remove(isA(PodPort.class));
    }
}
