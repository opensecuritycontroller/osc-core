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
package org.osc.core.broker.service.tasks.conformance.k8s.deploymentspec;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.InspectionPortElement;

public class DeleteK8sDAIInspectionPortTaskTest  {
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

    @Mock
    public ApiFactoryService apiFactoryServiceMock;

    @Mock
    public SdnRedirectionApi redirectionApi;

    @InjectMocks
    DeleteK8sDAIInspectionPortTask factory;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(this.em.getTransaction()).thenReturn(this.tx);

        this.txControl.setEntityManager(this.em);

        when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);
    }

    @Test
    public void testExecute_WhenSDNThrowsException_ThrowsUnhandledException() throws Exception {
        // Arrange.
        DistributedApplianceInstance dai = createAndRegisterDAI();
        DeleteK8sDAIInspectionPortTask task = this.factory.create(dai);

        doThrow(new IllegalStateException())
        .when(this.redirectionApi)
        .removeInspectionPort(argThat(new InspectionPortElementMatcher(dai.getInspectionElementId(), dai.getInspectionElementParentId())));

        when(this.apiFactoryServiceMock.createNetworkRedirectionApi(dai.getVirtualSystem())).thenReturn(this.redirectionApi);

        this.exception.expect(IllegalStateException.class);

        // Act.
        task.execute();

        // Assert.
        verify(this.em, never()).remove(any(DistributedApplianceInstance.class));
    }

    @Test
    public void testExecute_WhenInspectionPortIsRemoved_DAIIsDeleted() throws Exception {
        // Arrange.
        DistributedApplianceInstance dai = createAndRegisterDAI();
        DeleteK8sDAIInspectionPortTask task = this.factory.create(dai);
        when(this.apiFactoryServiceMock.createNetworkRedirectionApi(dai.getVirtualSystem())).thenReturn(this.redirectionApi);

        // Act.
        task.execute();

        // Assert.
        verify(this.redirectionApi, times(1))
        .removeInspectionPort(argThat(new InspectionPortElementMatcher(dai.getInspectionElementId(), dai.getInspectionElementParentId())));

        verify(this.em, times(1)).remove(dai);
    }

    private DistributedApplianceInstance createAndRegisterDAI() {
        DistributedApplianceInstance dai = new DistributedApplianceInstance();
        dai.setInspectionElementId(UUID.randomUUID().toString());
        dai.setInspectionElementParentId(UUID.randomUUID().toString());
        dai.setId(101L);

        when(this.em.find(DistributedApplianceInstance.class, dai.getId())).thenReturn(dai);

        return dai;
    }

    private class InspectionPortElementMatcher extends ArgumentMatcher<InspectionPortElement> {
        String inspElementId;
        String inspElementParentId;

        public InspectionPortElementMatcher(String inspElementId, String inspElementParentId) {
            this.inspElementId = inspElementId;
            this.inspElementParentId = inspElementParentId;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof InspectionPortElement)) {
                return false;
            }

            InspectionPortElement inspPortElement = (InspectionPortElement) object;
            return inspPortElement.getEgressPort() == null &&
                    inspPortElement.getIngressPort() == null &&
                    inspPortElement.getElementId().equals(this.inspElementId) &&
                    inspPortElement.getParentId().equals(this.inspElementParentId);
        }
    }
}
