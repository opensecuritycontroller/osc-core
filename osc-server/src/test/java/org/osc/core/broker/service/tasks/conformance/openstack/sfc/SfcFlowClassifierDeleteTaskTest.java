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
package org.osc.core.broker.service.tasks.conformance.openstack.sfc;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;
import org.osc.sdk.controller.api.SdnRedirectionApi;

@RunWith(MockitoJUnitRunner.class)
public class SfcFlowClassifierDeleteTaskTest {

    private static final String INSPECTION_HOOK_ID = "inspection-hook-id";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private EntityManager em;

    @Mock
    private EntityTransaction tx;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TestTransactionControl txControl;

    @Mock
    private DBConnectionManager dbMgr;

    @Mock
    private TransactionalBroadcastUtil txBroadcastUtil;

    @Mock
    private ApiFactoryService apiFactoryServiceMock;

    @Mock
    private SdnRedirectionApi sdnApi;

    @InjectMocks
    private SfcFlowClassifierDeleteTask task;

    private SecurityGroup sg;
    private ServiceFunctionChain sfc;
    private VMPort port;

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        Mockito.when(this.em.getTransaction()).thenReturn(this.tx);

        this.txControl.setEntityManager(this.em);

        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);

        VirtualizationConnector mockVc = mock(VirtualizationConnector.class);
        this.sg = new SecurityGroup(mockVc, "PROJECT_ID", "PROJECT_NAME");
        this.sg.setId(2L);
        this.sg.setName("A_SECURITY_GROUP");

        Mockito.when(this.em.find(SecurityGroup.class, this.sg.getId())).thenReturn(this.sg);

        this.sfc = new ServiceFunctionChain();
        this.sfc.setId(3L);
        this.sfc.setName("A_SFC_NAME");

        this.sg.setServiceFunctionChain(this.sfc);

        Mockito.when(this.em.find(ServiceFunctionChain.class, this.sfc.getId())).thenReturn(this.sfc);

        VM mockVm = mock(VM.class);
        when(mockVm.getName()).thenReturn("vm-name");

        this.port = mock(VMPort.class);

        when(this.port.getOpenstackId()).thenReturn("openstack-vmport-id");
        when(this.port.getVm()).thenReturn(mockVm);
        when(this.port.getInspectionHookId()).thenReturn(INSPECTION_HOOK_ID);

        Mockito.when(this.em.find(VMPort.class, this.port.getId())).thenReturn(this.port);

        Mockito.when(this.apiFactoryServiceMock.createNetworkRedirectionApi(mockVc)).thenReturn(this.sdnApi);

    }

    @Test
    public void testExecute_WithInspectionHook_ExpectUpdate() throws Exception {
        // Arrange
        SfcFlowClassifierDeleteTask deleteTask = this.task.create(this.sg, this.port);

        // Act.
        deleteTask.execute();

        // Assert
        verify(this.sdnApi, times(1)).removeInspectionHook(INSPECTION_HOOK_ID);
        verify(this.port).setInspectionHookId(null);
    }

    @Test
    public void testExecute_WithoutInspectionHook_ExpectSuccess() throws Exception {
        // Arrange
        when(this.port.getInspectionHookId()).thenReturn(null);

        SfcFlowClassifierDeleteTask deleteTask = this.task.create(this.sg, this.port);

        // Act.
        deleteTask.execute();

        // Assert
        verifyZeroInteractions(this.sdnApi);
        assertNull(this.port.getInspectionHookId());
    }

    @Test
    public void testExecute_WithInspectionHook_ExpectFailure() throws Exception {
        // Arrange
        doThrow(new Exception()).when(this.sdnApi).removeInspectionHook(INSPECTION_HOOK_ID);

        this.exception.expect(Exception.class);

        SfcFlowClassifierDeleteTask deleteTask = this.task.create(this.sg, this.port);

        // Act.
        deleteTask.execute();

    }

}
