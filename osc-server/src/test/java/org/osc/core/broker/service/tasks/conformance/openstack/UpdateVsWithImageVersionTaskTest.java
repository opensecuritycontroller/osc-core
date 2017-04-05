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

import java.util.Set;

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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.test.util.TestTransactionControl;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(HibernateUtil.class)
public class UpdateVsWithImageVersionTaskTest {
    @Rule public ExpectedException exception = ExpectedException.none();

    @Mock private EntityManager em;
    @Mock private EntityTransaction tx;

    @Mock(answer=Answers.CALLS_REAL_METHODS)
    TestTransactionControl txControl;

    private VirtualSystem vs;
    private ApplianceSoftwareVersion applianceSoftwareVersion;

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        Mockito.when(this.em.getTransaction()).thenReturn(this.tx);

        this.txControl.setEntityManager(this.em);

        PowerMockito.mockStatic(HibernateUtil.class);
        Mockito.when(HibernateUtil.getTransactionalEntityManager()).thenReturn(this.em);
        Mockito.when(HibernateUtil.getTransactionControl()).thenReturn(this.txControl);

        this.vs = new VirtualSystem();
        this.vs.setId(2L);
        this.vs.setName("vs");
        Appliance appliance = new Appliance();
        this.applianceSoftwareVersion = new ApplianceSoftwareVersion(appliance);
        this.applianceSoftwareVersion.setApplianceSoftwareVersion("applianceSoftwareVersion");
        this.vs.setApplianceSoftwareVersion(this.applianceSoftwareVersion);

        Mockito.when(this.em.find(Mockito.eq(VirtualSystem.class), Mockito.eq(this.vs.getId()),
                Mockito.eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(this.vs);
    }

    @Test
    public void testExecuteTransaction_WithNoImageReference_ExpectsNoUpdate() throws Exception {
        //Arrange.
        UpdateVsWithImageVersionTask task = new UpdateVsWithImageVersionTask(this.vs);

        //Act.
        task.execute();

        //Assert.
        Mockito.verify(this.em, Mockito.never()).merge(this.vs);
    }

    @Test
    public void testExecuteTransaction_WithImageWithApplianceVersion_ExpectsNoUpdate() throws Exception {
        //Arrange.
        OsImageReference imageReference = new OsImageReference(this.vs, "region", "refId");
        this.vs.addOsImageReference(imageReference);
        UpdateVsWithImageVersionTask task = new UpdateVsWithImageVersionTask(this.vs);

        //Act.
        task.execute();

        //Assert.
        Mockito.verify(this.em, Mockito.never()).merge(this.vs);
    }

    @Test
    public void testExecuteTransaction_WithImageWithoutApplianceVersion_ExpectsUpdate() throws Exception {
        //Arrange.
        OsImageReference imageReference = new OsImageReference(this.vs, "region", "refId");
        this.vs.addOsImageReference(imageReference);
        imageReference.setApplianceVersion(null);
        UpdateVsWithImageVersionTask task = new UpdateVsWithImageVersionTask(this.vs);

        //Act.
        task.execute();

        //Assert.
        Mockito.verify(this.em).merge(this.vs);
        Mockito.verify(this.em).merge(Mockito.argThat(new ImageReferenceHasVersionMatcher(this.vs)));
    }

    @Test
    public void testExecuteTransaction_WithMultipleImages_WithOneApplianceVersionNull_ExpectsUpdate() throws Exception {
        //Arrange.
        OsImageReference imageReference_1 = new OsImageReference(this.vs, "region", "refId1");
        OsImageReference imageReference_2 = new OsImageReference(this.vs, "region", "refId2");
        OsImageReference imageReference_3 = new OsImageReference(this.vs, "region", "refId3");
        OsImageReference imageReference_4 = new OsImageReference(this.vs, "region", "refId4");

        imageReference_1.setApplianceVersion(null);
        imageReference_2.setApplianceVersion(this.applianceSoftwareVersion);
        imageReference_3.setApplianceVersion(this.applianceSoftwareVersion);
        imageReference_4.setApplianceVersion(this.applianceSoftwareVersion);

        this.vs.addOsImageReference(imageReference_1);
        this.vs.addOsImageReference(imageReference_2);
        this.vs.addOsImageReference(imageReference_3);
        this.vs.addOsImageReference(imageReference_4);

        UpdateVsWithImageVersionTask task = new UpdateVsWithImageVersionTask(this.vs);

        //Act.
        task.execute();

        //Assert.
        Mockito.verify(this.em).merge(this.vs);
        Mockito.verify(this.em).merge(Mockito.argThat(new ImageReferenceHasVersionMatcher(this.vs)));
    }

    private class ImageReferenceHasVersionMatcher extends ArgumentMatcher<Object> {
        private VirtualSystem vs;

        ImageReferenceHasVersionMatcher(VirtualSystem vs) {
            this.vs = vs;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof VirtualSystem)) {
                return false;
            }

            boolean hasVersion = true;
            Set<OsImageReference> imageReferences = this.vs.getOsImageReference();
            for(OsImageReference imageReference : imageReferences) {
                if(!imageReference.getApplianceVersion().equals(this.vs.getApplianceSoftwareVersion())) {
                    hasVersion = false;
                }
            }
            return hasVersion;
        }
    }
}
