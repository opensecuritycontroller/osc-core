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
package org.osc.core.broker.service.tasks.conformance.openstack;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;

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
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.test.util.TestTransactionControl;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class DeleteImageReferenceTaskTest {
    @Rule public ExpectedException exception = ExpectedException.none();

    @Mock private EntityManager em;
    @Mock private EntityTransaction tx;

    @Mock(answer=Answers.CALLS_REAL_METHODS)
    TestTransactionControl txControl;

    @Mock
    DBConnectionManager dbMgr;

    @Mock
    TransactionalBroadcastUtil txBroadcastUtil;

    @InjectMocks
    DeleteImageReferenceTask factory;


    private OsImageReference imageReference;
    private OsImageReference otherImageReference;
    private VirtualSystem vs;
    private VirtualSystem otherVs;

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        Mockito.when(this.em.getTransaction()).thenReturn(this.tx);

        this.txControl.setEntityManager(this.em);

        Mockito.when(this.dbMgr.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(this.dbMgr.getTransactionControl()).thenReturn(this.txControl);

        this.vs = new VirtualSystem();
        this.vs.setId(1L);
        this.vs.setName("vs");
        this.imageReference = new OsImageReference(this.vs, "region", "refID");
        this.imageReference.setId(2L);
        this.vs.addOsImageReference(this.imageReference);

        this.otherVs = new VirtualSystem();
        this.otherVs.setId(3L);
        this.otherVs.setName("vs");
        this.otherImageReference = new OsImageReference(this.otherVs, "region", "refID");
        this.otherImageReference.setId(4L);
        this.otherVs.addOsImageReference(this.otherImageReference);

        Mockito.when(this.em.find(OsImageReference.class, this.imageReference.getId())).thenReturn(this.imageReference);
        Mockito.when(this.em.find(Mockito.eq(VirtualSystem.class), Mockito.eq(this.vs.getId()),
                Mockito.eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(this.vs);

        Mockito.when(this.em.find(OsImageReference.class, this.otherImageReference.getId())).thenReturn(this.otherImageReference);
        Mockito.when(this.em.find(Mockito.eq(VirtualSystem.class), Mockito.eq(this.otherVs.getId()),
                Mockito.eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(this.otherVs);
    }

    @Test
    public void testExecuteTransaction_WithImage_DeletesAndUpdatesVS() throws Exception {
        //Arrange.
        DeleteImageReferenceTask task = this.factory.create(this.imageReference, this.vs);

        //Act.
        task.execute();

        //Assert.
        Mockito.verify(this.em).remove(this.imageReference);
        Mockito.verify(this.em).merge(Mockito.argThat(new ImageReferenceIsEmptyMatcher(this.vs)));
        Mockito.verify(this.em).merge(this.vs);
    }

    @Test
    public void testExecuteTransaction_WithImageNotInVS_DeletesAndUpdatesVS() throws Exception {
        //Arrange.
        DeleteImageReferenceTask task = this.factory.create(this.otherImageReference, this.vs);

        //Act.
        task.execute();

        //Assert.
        Mockito.verify(this.em).remove(this.otherImageReference);
        Mockito.verify(this.em).merge(this.vs);
    }

    private class ImageReferenceIsEmptyMatcher extends ArgumentMatcher<Object> {
        private VirtualSystem vs;

        ImageReferenceIsEmptyMatcher(VirtualSystem vs) {
            this.vs = vs;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof VirtualSystem)) {
                return false;
            }

            return this.vs.getOsImageReference().isEmpty();
        }
    }
}
