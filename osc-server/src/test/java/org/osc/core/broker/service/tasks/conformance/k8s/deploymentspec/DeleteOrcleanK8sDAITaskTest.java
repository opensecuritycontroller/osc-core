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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;

public class DeleteOrcleanK8sDAITaskTest  {
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

    @InjectMocks
    DeleteOrCleanK8sDAITask factory;

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(this.em.getTransaction()).thenReturn(this.tx);

        this.txControl.setEntityManager(this.em);

        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);
    }

    @Test
    public void testExecute_WithDAIAssignedToInspectionElement_DAICleanedOfNetworkInformation() throws Exception {
        // Arrange.
        DistributedApplianceInstance dai = createAndRegisterDAI(true, 100L);

        DeleteOrCleanK8sDAITask task = this.factory.create(dai);

        // Act.
        task.execute();

        // Assert.
        verify(this.em, Mockito.times(1)).merge(Mockito.argThat(new CleanNetworkInfoDAIMatcher()));
        verify(this.em, Mockito.never()).remove(Mockito.any(DistributedApplianceInstance.class));
    }

    @Test
    public void testExecute_WithDAINotAssignedToInspectionElement_DAIDeleted() throws Exception {
        // Arrange.
        DistributedApplianceInstance dai = createAndRegisterDAI(false, 101L);

        DeleteOrCleanK8sDAITask task = this.factory.create(dai);

        // Act.
        task.execute();

        // Assert.
        verify(this.em, Mockito.times(1)).remove(dai);
    }

    private class CleanNetworkInfoDAIMatcher extends ArgumentMatcher<DistributedApplianceInstance> {
        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof DistributedApplianceInstance)) {
                return false;
            }

            DistributedApplianceInstance dai = (DistributedApplianceInstance) object;
            return dai.getInspectionOsIngressPortId() == null &&
                    dai.getInspectionIngressMacAddress() == null &&
                    dai.getInspectionEgressMacAddress() == null &&
                    dai.getInspectionOsEgressPortId() == null;
        }
    }

    private DistributedApplianceInstance createAndRegisterDAI(boolean withInspectionElement, long id) {
        DistributedApplianceInstance dai = new DistributedApplianceInstance();
        dai.setInspectionElementId(withInspectionElement ? UUID.randomUUID().toString() : null);
        dai.setInspectionElementParentId(withInspectionElement ? UUID.randomUUID().toString() : null);
        dai.setInspectionIngressMacAddress(UUID.randomUUID().toString());
        dai.setInspectionOsIngressPortId(UUID.randomUUID().toString());
        dai.setInspectionOsEgressPortId(UUID.randomUUID().toString());
        dai.setInspectionEgressMacAddress(UUID.randomUUID().toString());
        dai.setId(id);;

        when(this.em.find(DistributedApplianceInstance.class, dai.getId())).thenReturn(dai);

        return dai;
    }
}
