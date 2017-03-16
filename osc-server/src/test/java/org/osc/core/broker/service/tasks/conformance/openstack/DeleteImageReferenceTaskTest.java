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

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;

public class DeleteImageReferenceTaskTest {
    @Rule public ExpectedException exception = ExpectedException.none();

    @Mock private Session sessionMock;

    private OsImageReference imageReference;
    private OsImageReference otherImageReference;
    private VirtualSystem vs;
    private VirtualSystem otherVs;

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

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

        Mockito.when(this.sessionMock.get(OsImageReference.class, this.imageReference.getId())).thenReturn(this.imageReference);
        Mockito.when(this.sessionMock.get(Mockito.eq(VirtualSystem.class), Mockito.eq(this.vs.getId()),
                Mockito.argThat(new LockOptionsMatcher(LockMode.PESSIMISTIC_WRITE)))).thenReturn(this.vs);

        Mockito.when(this.sessionMock.get(OsImageReference.class, this.otherImageReference.getId())).thenReturn(this.otherImageReference);
        Mockito.when(this.sessionMock.get(Mockito.eq(VirtualSystem.class), Mockito.eq(this.otherVs.getId()),
                Mockito.argThat(new LockOptionsMatcher(LockMode.PESSIMISTIC_WRITE)))).thenReturn(this.otherVs);
    }

    @Test
    public void testExecuteTransaction_WithImage_DeletesAndUpdatesVS() throws Exception {
        //Arrange.
        DeleteImageReferenceTask task = new DeleteImageReferenceTask(this.imageReference, this.vs);

        //Act.
        task.executeTransaction(this.sessionMock);

        //Assert.
        Mockito.verify(this.sessionMock).delete(this.imageReference);
        Mockito.verify(this.sessionMock).update(Mockito.argThat(new ImageReferenceIsEmptyMatcher(this.vs)));
        Mockito.verify(this.sessionMock).update(this.vs);
    }

    @Test
    public void testExecuteTransaction_WithImageNotInVS_DeletesAndUpdatesVS() throws Exception {
        //Arrange.
        DeleteImageReferenceTask task = new DeleteImageReferenceTask(this.otherImageReference, this.vs);

        //Act.
        task.executeTransaction(this.sessionMock);

        //Assert.
        Mockito.verify(this.sessionMock).delete(this.otherImageReference);
        Mockito.verify(this.sessionMock).update(this.vs);
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

    private class LockOptionsMatcher extends ArgumentMatcher<LockOptions> {
        private LockMode lockMode;

        LockOptionsMatcher(LockMode lockMode) {
            this.lockMode = lockMode;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof LockOptions)) {
                return false;
            }

            return ((LockOptions) object).getLockMode().equals(this.lockMode);
        }
    }
}
