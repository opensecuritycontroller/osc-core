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

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.openstack.Network;
import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.util.db.HibernateUtil;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(HibernateUtil.class)
public class AllocateDAIWithSGIMembersTaskTest extends UpdateDAIToSGIMembersTaskTest {
    @InjectMocks
    AllocateDAIWithSGIMembersTask factory;

    @Test
    public void testExecute_WhenSGIHasNoAssociatedSG_NoUpdateIsDone() throws Exception {
        super.testExecute_WhenSGIHasNoAssociatedSG_NoUpdateIsDone(this.factory);
    }

    @Test
    public void testExecute_WhenSGIHasNoAssociatedSGMember_NoUpdateIsDone() throws Exception {
        super.testExecute_WhenSGIHasNoAssociatedSGMember_NoUpdateIsDone(this.factory);
    }

    @Test
    public void testExecute_WhenSGIHasVMSGMember_AllocatesDAIToVMPort() throws Exception {
        // Arrange.
        SecurityGroup sg = new SecurityGroup(null, null, null);
        sg.addSecurityGroupMember(newSGMWithPort(VM.class, 1L));

        SecurityGroupInterface sgi = registerNewSGI(sg);
        DistributedApplianceInstance dai = registerNewDAI();

        AllocateDAIWithSGIMembersTask task = this.factory.create(sgi, dai);

        // Act.
        task.execute();

        // Assert.
        assertExpectedProtectedPorts(dai);
    }

    @Test
    public void testExecute_WhenSGIHasNetworkSGMember_AllocatesDAIToVMPort() throws Exception {
        // Arrange.
        SecurityGroup sg = new SecurityGroup(null, null, null);
        sg.addSecurityGroupMember(newSGMWithPort(Network.class, 1L));

        SecurityGroupInterface sgi = registerNewSGI(sg);
        DistributedApplianceInstance dai = registerNewDAI();

        AllocateDAIWithSGIMembersTask task = this.factory.create(sgi, dai);

        // Act.
        task.execute();

        // Assert.
        assertExpectedProtectedPorts(dai);
    }

    @Test
    public void testExecute_WhenSGIHasSubNetSGMember_AllocatesDAIToVMPort() throws Exception {
        // Arrange.
        SecurityGroup sg = new SecurityGroup(null, null, null);
        sg.addSecurityGroupMember(newSGMWithPort(Subnet.class, 1L));

        SecurityGroupInterface sgi = registerNewSGI(sg);
        DistributedApplianceInstance dai = registerNewDAI();
        AllocateDAIWithSGIMembersTask task = this.factory.create(sgi, dai);

        // Act.
        task.execute();

        // Assert.
        assertExpectedProtectedPorts(dai);
    }

    @Test
    public void testExecute_WhenSGIHasMultipleSGMembers_AllocatesDAIToVMPorts() throws Exception {
        // Arrange.
        SecurityGroup sg = new SecurityGroup(null, null, null);
        sg.addSecurityGroupMember(newSGMWithPort(VM.class, 1L));
        sg.addSecurityGroupMember(newSGMWithPort(VM.class, 2L));
        sg.addSecurityGroupMember(newSGMVmWithoutPort(3L));

        SecurityGroupInterface sgi = registerNewSGI(sg);
        DistributedApplianceInstance dai = registerNewDAI();

        AllocateDAIWithSGIMembersTask task = this.factory.create(sgi, dai);

        // Act.
        task.execute();

        // Assert.
        assertExpectedProtectedPorts(dai);
    }

    @Test
    public void testExecute_WhenSGIHasSingleSGMemberWithMultiplePorts_AllocatesDAIToVMPorts() throws Exception {
        // Arrange.
        SecurityGroup sg = new SecurityGroup(null, null, null);
        SecurityGroupMember sgm = newSGMVmWithoutPort(1L);
        sg.addSecurityGroupMember(sgm);

        VM vm = sgm.getVm();

        this.protectedPorts.add(newVMPort(vm));
        this.protectedPorts.add(newVMPort(vm));
        this.protectedPorts.add(newVMPort(vm));

        SecurityGroupInterface sgi = registerNewSGI(sg);
        DistributedApplianceInstance dai = registerNewDAI();

        AllocateDAIWithSGIMembersTask task = this.factory.create(sgi, dai);

        // Act.
        task.execute();

        // Assert.
        assertExpectedProtectedPorts(dai);
    }

    private void assertExpectedProtectedPorts(DistributedApplianceInstance dai) {
        List<VMPort> portsMissingDai = this.protectedPorts.stream().filter(port -> !port.getDais().contains(dai))
                .collect(Collectors.toList());

        Assert.assertTrue(
                String.format("The following ports %s were found without a dai.", String.join(",",
                        portsMissingDai.stream().map(port -> port.getOsNetworkId()).collect(Collectors.toList()))),
                portsMissingDai.isEmpty());

        Object[] expectedProtectedPorts = this.protectedPorts.stream()
                .sorted(Comparator.comparing(VMPort::getOsNetworkId)).toArray(size -> new VMPort[size]);

        VMPort[] actualProtectedPorts = dai.getProtectedPorts().stream()
                .sorted(Comparator.comparing(VMPort::getOsNetworkId)).toArray(size -> new VMPort[size]);

        Assert.assertArrayEquals("The expected ports are different than the dai ports.", expectedProtectedPorts,
                actualProtectedPorts);

        Mockito.verify(this.em, Mockito.times(this.protectedPorts.size())).merge(dai);
        Mockito.verify(this.em, Mockito.times(this.protectedPorts.size())).merge(Mockito.isA(VMPort.class));
        this.protectedPorts.forEach(port -> Mockito.verify(this.em).merge(port));
    }
}
