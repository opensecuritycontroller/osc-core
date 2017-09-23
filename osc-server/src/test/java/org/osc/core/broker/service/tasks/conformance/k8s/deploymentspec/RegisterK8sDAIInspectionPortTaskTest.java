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

import org.junit.Assert;
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
import org.osc.sdk.controller.DefaultInspectionPort;
import org.osc.sdk.controller.api.SdnRedirectionApi;
import org.osc.sdk.controller.element.InspectionPortElement;

public class RegisterK8sDAIInspectionPortTaskTest  {
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
    RegisterK8sDAIInspectionPortTask factory;

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
        RegisterK8sDAIInspectionPortTask task = this.factory.create(dai);

        mockRegisterInspectionPortNetworkElement(dai, new IllegalStateException());
        this.exception.expect(IllegalStateException.class);

        // Act.
        task.execute();

        // Assert.
        verify(this.em, never()).merge(any(DistributedApplianceInstance.class));
    }

    @Test
    public void testExecute_WhenInspectionPortIsRegistered_DAIIsUpdated() throws Exception {
        // Arrange.
        DistributedApplianceInstance dai = createAndRegisterDAI();
        RegisterK8sDAIInspectionPortTask task = this.factory.create(dai);

        String inspectionPortId = mockRegisterInspectionPortNetworkElement(dai);

        // Act.
        task.execute();

        // Assert.
        Assert.assertEquals("The inspection element id was different than expected.", inspectionPortId, dai.getInspectionElementId());
        verify(this.em, times(1)).merge(dai);
    }

    private String mockRegisterInspectionPortNetworkElement(DistributedApplianceInstance dai) throws Exception {
        String inspectionPortId = UUID.randomUUID().toString();

        DefaultInspectionPort defaultInspPort = new DefaultInspectionPort(null, null, inspectionPortId, null);

        when(this.redirectionApi.registerInspectionPort(argThat(new InspectionPortElementMatcher(dai)))).thenReturn(defaultInspPort);
        when(this.apiFactoryServiceMock.createNetworkRedirectionApi(dai.getVirtualSystem())).thenReturn(this.redirectionApi);

        return inspectionPortId;
    }

    private void mockRegisterInspectionPortNetworkElement(DistributedApplianceInstance dai, Exception throwException) throws Exception {
        when(this.redirectionApi.registerInspectionPort(argThat(new InspectionPortElementMatcher(dai)))).thenThrow(throwException);
        when(this.apiFactoryServiceMock.createNetworkRedirectionApi(dai.getVirtualSystem())).thenReturn(this.redirectionApi);
    }

    private DistributedApplianceInstance createAndRegisterDAI() {
        DistributedApplianceInstance dai = new DistributedApplianceInstance();
        dai.setInspectionElementId(UUID.randomUUID().toString());
        dai.setInspectionElementParentId(UUID.randomUUID().toString());
        dai.setId(101L);
        dai.setInspectionOsIngressPortId(UUID.randomUUID().toString());
        dai.setInspectionOsEgressPortId(UUID.randomUUID().toString());
        when(this.em.find(DistributedApplianceInstance.class, dai.getId())).thenReturn(dai);

        return dai;
    }

    private class InspectionPortElementMatcher extends ArgumentMatcher<InspectionPortElement> {
        DistributedApplianceInstance dai;

        public InspectionPortElementMatcher(DistributedApplianceInstance dai) {
            this.dai = dai;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof InspectionPortElement)) {
                return false;
            }

            InspectionPortElement inspPortElement = (InspectionPortElement) object;
            return inspPortElement.getEgressPort().getElementId().equals(this.dai.getInspectionOsEgressPortId()) &&
                    inspPortElement.getIngressPort().getElementId().equals(this.dai.getInspectionOsIngressPortId()) &&
                    inspPortElement.getElementId().equals(this.dai.getInspectionElementId()) &&
                    inspPortElement.getParentId().equals(this.dai.getInspectionElementParentId());
        }
    }
}
