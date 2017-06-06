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

import static org.mockito.Mockito.when;

import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.TagEncapsulationType;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.FailurePolicyType;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.InspectionPortElement;

public class BasePortGroupHookTaskTest {
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

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(this.em.getTransaction()).thenReturn(this.tx);

        this.txControl.setEntityManager(this.em);

        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);
    }

    protected void testExecute_WhenSGHasNoNetworkElementId_ThrowsValidationException(BasePortGroupHookTask factory)
            throws Exception {
        // Arrange.
        SecurityGroup sg = newSecurityGroup();
        sg.setNetworkElementId(null);
        SecurityGroupInterface sgi = registerNewSGI(sg, 1L);
        DistributedApplianceInstance dai = registerNewDAI();

        this.exception.expect(VmidcBrokerValidationException.class);
        this.exception.expectMessage(
                String.format("The security group %s does not have a network element set.", sg.getName()));

        BasePortGroupHookTask task = factory.create(sgi, dai);

        // Act.
        task.execute();
    }

    protected void testExecute_WhenCreateNetworkRedirectionApiFails_ThrowsTheUnhandledException(
            BasePortGroupHookTask factory) throws Exception {
        // Arrange.
        SecurityGroupInterface sgi = registerNewSGI(newSecurityGroup(), 2L);
        DistributedApplianceInstance dai = registerNewDAI();

        when(this.apiFactoryServiceMock.createNetworkRedirectionApi(sgi.getVirtualSystem()))
                .thenThrow(new IllegalStateException());

        this.exception.expect(IllegalStateException.class);

        BasePortGroupHookTask task = factory.create(sgi, dai);

        // Act.
        task.execute();
    }

    protected void registerNetworkRedirectionApi(SdnRedirectionApi redirectionApi, VirtualSystem vs) throws Exception {
        when(this.apiFactoryServiceMock.createNetworkRedirectionApi(vs)).thenReturn(redirectionApi);
    }

    protected SecurityGroup newSecurityGroup() {
        SecurityGroup sg = new SecurityGroup(null, null, null);
        sg.setNetworkElementId(UUID.randomUUID().toString());
        sg.setName(UUID.randomUUID().toString());
        return sg;
    }

    protected SecurityGroupInterface registerNewSGI(SecurityGroup sg, Long entityId) {
        VirtualSystem vs = new VirtualSystem(null);
        vs.setId(entityId);
        vs.setEncapsulationType(TagEncapsulationType.VLAN);

        SecurityGroupInterface sgi = new SecurityGroupInterface(vs, null, entityId.toString(),
                FailurePolicyType.FAIL_CLOSE, 1L);
        sgi.setId(entityId);

        if (sg != null) {
            sgi.addSecurityGroup(sg);
        }

        when(this.em.find(SecurityGroupInterface.class, sgi.getId())).thenReturn(sgi);
        return sgi;
    }

    protected DistributedApplianceInstance registerNewDAI() {
        DistributedApplianceInstance dai = new DistributedApplianceInstance();
        dai.setName("dai-name");
        dai.setId(1L);
        dai.setInspectionEgressMacAddress(UUID.randomUUID().toString());
        dai.setInspectionOsEgressPortId(UUID.randomUUID().toString());
        dai.setInspectionIngressMacAddress(UUID.randomUUID().toString());
        dai.setInspectionOsIngressPortId(UUID.randomUUID().toString());

        when(this.em.find(DistributedApplianceInstance.class, dai.getId())).thenReturn(dai);
        return dai;
    }

    protected boolean inspectionPortMatchesDAI(InspectionPortElement inspectionPort, DistributedApplianceInstance dai) {
        return inspectionPort.getIngressPort().getMacAddresses().size() == 1
                && inspectionPort.getIngressPort().getMacAddresses().get(0).equals(dai.getInspectionIngressMacAddress())
                && inspectionPort.getIngressPort().getElementId().equals(dai.getInspectionOsIngressPortId())
                && inspectionPort.getEgressPort().getMacAddresses().size() == 1
                && inspectionPort.getEgressPort().getMacAddresses().get(0).equals(dai.getInspectionEgressMacAddress())
                && inspectionPort.getEgressPort().getElementId().equals(dai.getInspectionOsEgressPortId());
    }
}
