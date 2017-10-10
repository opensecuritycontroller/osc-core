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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentMatcher;
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
import org.osc.sdk.controller.element.InspectionHookElement;

@RunWith(MockitoJUnitRunner.class)
public class SfcFlowClassifierUpdateTaskTest {

    private static final String OPENSTACK_VMPORT_ID = "openstack-vmport-id";

    private static final String OPENSTACK_SFC_ID = "openstack-sfc-id";

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
    private SfcFlowClassifierUpdateTask task;

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
        this.sg.setNetworkElementId(OPENSTACK_SFC_ID);

        Mockito.when(this.em.find(SecurityGroup.class, this.sg.getId())).thenReturn(this.sg);

        this.sfc = new ServiceFunctionChain();
        this.sfc.setId(3L);
        this.sfc.setName("A_SFC_NAME");

        this.sg.setServiceFunctionChain(this.sfc);

        VM mockVm = mock(VM.class);
        when(mockVm.getName()).thenReturn("vm-name");

        this.port = mock(VMPort.class);

        when(this.port.getId()).thenReturn(1L);
        when(this.port.getOpenstackId()).thenReturn(OPENSTACK_VMPORT_ID);
        when(this.port.getVm()).thenReturn(mockVm);
        when(this.port.getInspectionHookId()).thenReturn(INSPECTION_HOOK_ID);

        Mockito.when(this.em.find(VMPort.class, this.port.getId())).thenReturn(this.port);
        Mockito.when(this.apiFactoryServiceMock.createNetworkRedirectionApi(mockVc)).thenReturn(this.sdnApi);

    }

    @Test
    public void testExecute_WithInspectionHook_ExpectUpdate() throws Exception {
        // Arrange
        SfcFlowClassifierUpdateTask updateTask = this.task.create(this.sg, this.port);

        // Act.
        updateTask.execute();

        // Assert
        verify(this.sdnApi, times(1)).updateInspectionHook(
                argThat(new InspectionHookElementMatcher(INSPECTION_HOOK_ID, OPENSTACK_VMPORT_ID, OPENSTACK_SFC_ID)));
        verify(this.port, times(0)).setInspectionHookId(any());
    }

    @Test
    public void testExecute_WithInspectionHook_ExpectFailure() throws Exception {
        // Arrange
        doThrow(new Exception()).when(this.sdnApi).updateInspectionHook(any());

        this.exception.expect(Exception.class);

        SfcFlowClassifierUpdateTask updateTask = this.task.create(this.sg, this.port);

        // Act.
        updateTask.execute();

    }

    private static class InspectionHookElementMatcher extends ArgumentMatcher<InspectionHookElement> {

        private String hookId;
        private String inspectedPortId;
        private String inspectionPortId;

        public InspectionHookElementMatcher(String hookId, String inspectedPortId, String inspectionPortId) {
            this.hookId = hookId;
            this.inspectedPortId = inspectedPortId;
            this.inspectionPortId = inspectionPortId;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof InspectionHookElement)) {
                return false;
            }
            InspectionHookElement otherHook = (InspectionHookElement) object;
            return this.hookId.equals(otherHook.getHookId())
                    && this.inspectedPortId.equals(otherHook.getInspectedPort().getElementId())
                    && this.inspectionPortId.equals(otherHook.getInspectionPort().getElementId());
        }
    }

}
