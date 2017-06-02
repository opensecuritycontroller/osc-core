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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;

import org.junit.Assert;
import org.junit.Before;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.openstack.Network;
import org.osc.core.broker.model.entities.virtualization.openstack.OsProtectionEntity;
import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.test.util.TestTransactionControl;
import org.powermock.api.mockito.PowerMockito;

public class UpdateDAIToSGIMembersTaskTest {
    @Mock protected EntityManager em;
    @Mock protected EntityTransaction tx;

    @Mock(answer=Answers.CALLS_REAL_METHODS)
    TestTransactionControl txControl;

    protected List<VMPort> protectedPorts;

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);
        Mockito.when(this.em.getTransaction()).thenReturn(this.tx);

        this.txControl.setEntityManager(this.em);

        PowerMockito.mockStatic(HibernateUtil.class);
        Mockito.when(HibernateUtil.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(HibernateUtil.getTransactionControl()).thenReturn(this.txControl);
        this.protectedPorts = new ArrayList<>();
    }

    public void testExecute_WhenSGIHasNoAssociatedSG_NoUpdateIsDone() throws Exception {
        // Arrange.
        SecurityGroupInterface sgi = registerNewSGI(null);
        DistributedApplianceInstance dai = registerNewDAI();

        AllocateDAIWithSGIMembersTask task = new AllocateDAIWithSGIMembersTask(sgi, dai);

        // Act.
        task.executeTransaction(this.em);

        // Assert.
        Mockito.verify(this.em, Mockito.never()).merge(Mockito.any());
        Assert.assertTrue("The dai should not have associated protected ports.", dai.getProtectedPorts().isEmpty());
    }

    public void testExecute_WhenSGIHasNoAssociatedSGMember_NoUpdateIsDone() throws Exception {
        // Arrange.
        SecurityGroupInterface sgi = registerNewSGI(new SecurityGroup(null, null, null));
        DistributedApplianceInstance dai = registerNewDAI();

        AllocateDAIWithSGIMembersTask task = new AllocateDAIWithSGIMembersTask(sgi, dai);

        // Act.
        task.execute();

        // Assert.
        Mockito.verify(this.em, Mockito.never()).merge(Mockito.any());
        Assert.assertTrue("The dai should not have associated protected ports.", dai.getProtectedPorts().isEmpty());
    }

    protected DistributedApplianceInstance registerNewDAI() {
        DistributedApplianceInstance dai = new DistributedApplianceInstance();
        dai.setName("dai-name");
        dai.setId(1L);

        Mockito.when(this.em.find(DistributedApplianceInstance.class, dai.getId(), LockModeType.PESSIMISTIC_WRITE)).thenReturn(dai);
        return dai;
    }

    protected SecurityGroupInterface registerNewSGI(SecurityGroup sg) {
        SecurityGroupInterface sgi = new SecurityGroupInterface();
        sgi.setId(1L);
        if (sg != null) {
            sgi.addSecurityGroup(sg);
        }

        Mockito.when(this.em.find(SecurityGroupInterface.class, sgi.getId())).thenReturn(sgi);
        return sgi;
    }

    protected SecurityGroupMember newSGMWithPort(Class<? extends OsProtectionEntity> entityType, Long sgmId) {
        VMPort port = null;
        OsProtectionEntity protectionEntity;

        if (entityType == VM.class) {
            protectionEntity = new VM("region", UUID.randomUUID().toString(), "name");
            port = newVMPort((VM)protectionEntity);
        } else if (entityType == Network.class) {
            protectionEntity = new Network("region", UUID.randomUUID().toString(), "name");
            port = new VMPort((Network)protectionEntity, "mac-address", UUID.randomUUID().toString(), UUID.randomUUID().toString(), null);
        } else {
            protectionEntity = new Subnet("network", UUID.randomUUID().toString(), "name", "region", false);
            port = new VMPort((Subnet)protectionEntity, "mac-address", UUID.randomUUID().toString(), UUID.randomUUID().toString(), null);
        }

        this.protectedPorts.add(port);
        return newSGM(protectionEntity, sgmId);
    }

    protected SecurityGroupMember newSGMVmWithoutPort(Long sgmId) {
        OsProtectionEntity protectionEntity;
        protectionEntity = new VM("region", UUID.randomUUID().toString(), "name");

        return newSGM(protectionEntity, sgmId);
    }

    protected VMPort newVMPort(VM vm) {
        return new VMPort(vm, "mac-address", UUID.randomUUID().toString(), UUID.randomUUID().toString(), null);
    }

    protected SecurityGroupMember newSGM(OsProtectionEntity protectionEntity, Long sgmId) {
        // TODO emanoel: Remove this mock once the SGM is no longer kept in a TreeSet in the SGM.
        SecurityGroupMember sgm = Mockito.spy(new SecurityGroupMember(protectionEntity));
        Mockito.doReturn(-1).when(sgm).compareTo(Mockito.any());
        sgm.setId(sgmId);
        return sgm;
    }
}
