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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.sdn.NetworkElementImpl;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;
import org.osc.core.test.util.mockito.matchers.ElementIdMatcher;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.NetworkElement;

@RunWith(MockitoJUnitRunner.class)
public class UpdateServiceFunctionChainTaskTest {

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

    @Captor
    private ArgumentCaptor<List<NetworkElement>> neListCaptor;

    @InjectMocks
    private UpdateServiceFunctionChainTask task;

    private SecurityGroup sg;
    private ServiceFunctionChain sfc;
    private List<NetworkElement> networkElementList;

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

        Mockito.when(this.apiFactoryServiceMock.createNetworkRedirectionApi(mockVc)).thenReturn(this.sdnApi);

    }

    @Test
    public void testExecute_WithSecurityGroupSFCIdSetToNull_ExpectUpdate() throws Exception {
        // Arrange
        UpdateServiceFunctionChainTask updateTask = this.task.create(this.sg, this.networkElementList);
        this.sg.setNetworkElementId(null);
        Mockito.when(this.sdnApi
                .updateNetworkElement(argThat(new ElementIdMatcher<NetworkElement>(this.sg.getNetworkElementId())), any()))
                .thenReturn(new NetworkElementImpl("UPDATED_SFC_ID"));

        // Act.
        updateTask.execute();

        // Assert
        assertEquals(this.sg.getNetworkElementId(), "UPDATED_SFC_ID");
    }

    @Test
    public void testExecute_WithSecurityGroupSFCIdSetToNotEmpty_ExpectUpdate() throws Exception {
        // Arrange
        this.sg.setNetworkElementId("OLD_SFC_ID");
        UpdateServiceFunctionChainTask updateTask = this.task.create(this.sg, this.networkElementList);
        Mockito.when(this.sdnApi
                .updateNetworkElement(argThat(new ElementIdMatcher<NetworkElement>(this.sg.getNetworkElementId())), any()))
                .thenReturn(new NetworkElementImpl("UPDATED_SFC_ID"));
        // Act.
        updateTask.execute();

        // Assert
        assertEquals(this.sg.getNetworkElementId(), "UPDATED_SFC_ID");
    }

    @Test
    public void testExecute_CallsSDNUpdateWithProvidedParameters_ExpectUpdate() throws Exception {

        // Arrange
        this.sg.setNetworkElementId("OLD_SFC_ID");
        this.networkElementList = new ArrayList<>();

        UpdateServiceFunctionChainTask updateTask = this.task.create(this.sg, this.networkElementList);

        Mockito.when(this.sdnApi.updateNetworkElement(
                argThat(new ElementIdMatcher<NetworkElement>(this.sg.getNetworkElementId())), this.neListCaptor.capture()))
                .thenReturn(new NetworkElementImpl("UPDATED_SFC_ID"));
        // Act.
        updateTask.execute();

        // Assert
        assertEquals(this.networkElementList, this.neListCaptor.getValue());
    }

    @Test
    public void testGetName_WithSecurityGroupSFCId_ExpectCorrect() {
        // Arrange
        UpdateServiceFunctionChainTask updateTask = this.task.create(this.sg, this.networkElementList);

        // Act.
        String taskName = updateTask.getName();

        // Assert
        assertEquals(String.format("Updating Service Function Chain '%s' for Security Group '%s' under Project '%s'",
                this.sfc.getName(), this.sg.getName(), this.sg.getProjectName()), taskName);
    }

}
