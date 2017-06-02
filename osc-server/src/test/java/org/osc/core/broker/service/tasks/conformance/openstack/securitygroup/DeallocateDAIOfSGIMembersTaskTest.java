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

import java.util.List;
import java.util.UUID;
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
public class DeallocateDAIOfSGIMembersTaskTest extends UpdateDAIToSGIMembersTaskTest {
    @InjectMocks
    DeallocateDAIOfSGIMembersTask factory;

    @Test
    public void testExecute_WhenSGIHasNoAssociatedSG_NoUpdateIsDone() throws Exception {
        super.testExecute_WhenSGIHasNoAssociatedSG_NoUpdateIsDone(this.factory);
    }

    @Test
    public void testExecute_WhenSGIHasNoAssociatedSGMember_NoUpdateIsDone() throws Exception {
        super.testExecute_WhenSGIHasNoAssociatedSGMember_NoUpdateIsDone(this.factory);
    }

    @Test
    public void testExecute_WhenSGIHasVMSGMember_DeallocatesDAIOfVMPort() throws Exception {
        // Arrange.
        SecurityGroup sg = new SecurityGroup(null, null, null);
        sg.addSecurityGroupMember(newSGMWithPort(VM.class, 1L));

        SecurityGroupInterface sgi = registerNewSGI(sg);
        DistributedApplianceInstance dai = registerNewDAI();

        assignDAIToProtectedPorts(dai);

        DeallocateDAIOfSGIMembersTask task = this.factory.create(sgi, dai);

        // Act.
        task.execute();

        // Assert.
        assertExpectedProtectedPorts(dai);
    }

    @Test
    public void testExecute_WhenDaiHasOtherProtectedPorts_ExtraProtectedPortRemains() throws Exception {
        // Arrange.
        SecurityGroup sg = new SecurityGroup(null, null, null);
        sg.addSecurityGroupMember(newSGMWithPort(VM.class, 1L));

        SecurityGroupInterface sgi = registerNewSGI(sg);
        DistributedApplianceInstance dai = registerNewDAI();

        assignDAIToProtectedPorts(dai);
        VMPort extraPort = newVMPort(new VM("region", UUID.randomUUID().toString(), "name"));
        dai.addProtectedPort(extraPort);

        DeallocateDAIOfSGIMembersTask task = this.factory.create(sgi, dai);

        // Act.
        task.execute();

        // Assert.
        int expectedRemainingProtectedPortCount = dai.getProtectedPorts().size();
        assertExpectedProtectedPorts(dai);
        Assert.assertEquals(String.format("The DAI should have only %s port.", expectedRemainingProtectedPortCount),
                expectedRemainingProtectedPortCount, dai.getProtectedPorts().size());
        Assert.assertTrue(String.format("The DAI should have the protected port %s.", extraPort.getOpenstackId()),
                dai.getProtectedPorts().contains(extraPort));
    }

    @Test
    public void testExecute_WhenSGIHasNetworkSGMember_DeallocatesDAIsOfVMPort() throws Exception {
        // Arrange.
        SecurityGroup sg = new SecurityGroup(null, null, null);
        sg.addSecurityGroupMember(newSGMWithPort(Network.class, 1L));

        SecurityGroupInterface sgi = registerNewSGI(sg);
        DistributedApplianceInstance dai = registerNewDAI();

        assignDAIToProtectedPorts(dai);

        DeallocateDAIOfSGIMembersTask task = this.factory.create(sgi, dai);

        // Act.
        task.execute();

        // Assert.
        assertExpectedProtectedPorts(dai);
    }

    @Test
    public void testExecute_WhenSGIHasSubNetSGMember_DeallocatesDAIsOfVMPort() throws Exception {
        // Arrange.
        SecurityGroup sg = new SecurityGroup(null, null, null);
        sg.addSecurityGroupMember(newSGMWithPort(Subnet.class, 1L));

        SecurityGroupInterface sgi = registerNewSGI(sg);
        DistributedApplianceInstance dai = registerNewDAI();

        assignDAIToProtectedPorts(dai);

        DeallocateDAIOfSGIMembersTask task = this.factory.create(sgi, dai);

        // Act.
        task.execute();

        // Assert.
        assertExpectedProtectedPorts(dai);
    }

    @Test
    public void testExecute_WhenSGIHasMultipleSGMembers_DeallocatesDAIsOfVMPorts() throws Exception {
        // Arrange.
        SecurityGroup sg = new SecurityGroup(null, null, null);
        sg.addSecurityGroupMember(newSGMWithPort(VM.class, 1L));
        sg.addSecurityGroupMember(newSGMWithPort(VM.class, 2L));
        sg.addSecurityGroupMember(newSGMVmWithoutPort(3L));

        SecurityGroupInterface sgi = registerNewSGI(sg);
        DistributedApplianceInstance dai = registerNewDAI();

        assignDAIToProtectedPorts(dai);

        DeallocateDAIOfSGIMembersTask task = this.factory.create(sgi, dai);

        // Act.
        task.execute();

        // Assert.
        assertExpectedProtectedPorts(dai);
    }

    @Test
    public void testExecute_WhenSGIHasSingleSGMemberWithMultiplePorts_DeallocatesDAIsOfVMPorts() throws Exception {
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

        assignDAIToProtectedPorts(dai);

        DeallocateDAIOfSGIMembersTask task = this.factory.create(sgi, dai);

        // Act.
        task.execute();

        // Assert.
        assertExpectedProtectedPorts(dai);
    }

    private void assertExpectedProtectedPorts(DistributedApplianceInstance dai) {
        List<VMPort> portsWithDai = this.protectedPorts.stream().filter(port -> port.getDais().contains(dai))
                .collect(Collectors.toList());

        Assert.assertTrue(
                String.format("The following ports %s should not have an assigned DAI.",
                        String.join(",",
                                portsWithDai.stream().map(port -> port.getOsNetworkId()).collect(Collectors.toList()))),
                portsWithDai.isEmpty());

        Mockito.verify(this.em, Mockito.times(this.protectedPorts.size())).merge(dai);
        Mockito.verify(this.em, Mockito.times(this.protectedPorts.size())).merge(Mockito.isA(VMPort.class));
        this.protectedPorts.forEach(port -> Mockito.verify(this.em).merge(port));
        this.protectedPorts.forEach(port -> Assert.assertTrue(
                String.format("The dai should not contain the port %s.", port.getOpenstackId()),
                !dai.getProtectedPorts().contains(port)));
    }

    private void assignDAIToProtectedPorts(DistributedApplianceInstance dai) {
        for (VMPort protectedPort : this.protectedPorts) {
            dai.addProtectedPort(protectedPort);
            protectedPort.addDai(dai);
        }
    }
}
