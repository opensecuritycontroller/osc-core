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

import static org.osc.core.broker.service.tasks.conformance.openstack.OsImageCheckMetaTaskTestData.*;

import java.util.Arrays;
import java.util.Collection;

import org.hibernate.Session;
import org.jclouds.openstack.glance.v1_0.domain.Image.Status;
import org.jclouds.openstack.glance.v1_0.domain.ImageDetails;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudGlance;
import org.osc.core.test.util.TaskGraphHelper;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
public class OsImageCheckMetaTaskTest {
    @Mock private Session sessionMock;
    @Mock private JCloudGlance glanceMock;

    private VirtualSystem vs;
    private String vcName;
    private String region;
    private Endpoint osEndPoint;

    private TaskGraph expectedGraph;

    public OsImageCheckMetaTaskTest(VirtualSystem vs, String region, Endpoint osEndPoint, TaskGraph tg) {
        this.vs = vs;
        this.vcName = vs.getVirtualizationConnector().getName();
        this.osEndPoint = osEndPoint;
        this.region = region;
        this.expectedGraph = tg;
    }

    @Before
    public void testInitialize() throws Exception {
        MockitoAnnotations.initMocks(this);

        for (VirtualSystem vc : TEST_VIRTUAL_SYSTEMS) {
            Mockito.doReturn(this.vs).when(this.sessionMock).get(VirtualSystem.class, this.vs.getId());
        }

        Mockito.doReturn(null).when(this.glanceMock).getImageById(REGION_ONE, SINGLE_REF_ID);
        registerImage(false, false, REGION_TWO, SINGLE_REF_ID);
        registerImage(true, false, REGION_THREE, SINGLE_REF_ID);
        registerImage(true, true, REGION_FOUR, SINGLE_REF_ID);
        registerImage(false, false, REGION_FIVE, REF_ID_ONE);
        registerImage(true, false, REGION_FIVE, REF_ID_TWO);
        registerImage(false, true, REGION_FIVE, REF_ID_THREE);
    }

    @Test
    public void testExecuteTransaction_WithVariousVirtualSystems_ExpectsCorrectTaskGraph() throws Exception {
        //Arrange.
        OsImageCheckMetaTask task = new OsImageCheckMetaTask(this.vs, this.region, this.osEndPoint, this.glanceMock);

        //Act.
        task.executeTransaction(this.sessionMock);

        //Assert.
        TaskGraphHelper.validateTaskGraph(task, this.expectedGraph);
    }

    @Parameters()
    public static Collection<Object[]> getTestData() {
        return Arrays.asList(new Object[][] {
            {VS_WITHOUT_IMAGE_REFERENCE, REGION, null, emptyGraph(VS_WITHOUT_IMAGE_REFERENCE)},
            {VS_WITH_NULL_IMAGE, REGION_ONE, null, deleteImageFromDBAndUploadToGlanceGraph(VS_WITH_NULL_IMAGE)},
            {VS_WITH_INACTIVE_IMAGE_STATUS, REGION_TWO, null, deleteImageFromDBAndUploadToGlanceGraph(VS_WITH_INACTIVE_IMAGE_STATUS)},
            {VS_WITH_UNEXPECTED_IMAGE_NAME, REGION_THREE, null, deleteImageFromGlanceAndUploadToGlanceGraph(VS_WITH_UNEXPECTED_IMAGE_NAME)},
            {VS_WITH_UNEXPECTED_REGION, UNEXPECTED_REGION, null, updateVSWithImageVersionGraph(VS_WITH_UNEXPECTED_REGION)},
            {VS_WITH_MULTIPLE_IMAGES, REGION_FIVE, null, deleteImagesAndUploadToGlance(VS_WITH_MULTIPLE_IMAGES)}
        });
    }

    private void registerImage(boolean isActive, boolean hasExpectedName, String region, String refId) throws Exception {
        ImageDetails imageMock = Mockito.mock(ImageDetails.class);
        Mockito.doReturn(imageMock).when(this.glanceMock).getImageById(region, refId);
        if(isActive) {
            Mockito.doReturn(Status.ACTIVE).when(imageMock).getStatus();
        } else {
            Mockito.doReturn(Status.DELETED).when(imageMock).getStatus();
        }
        if(!hasExpectedName) {
            Mockito.doReturn("unexpected name").when(imageMock).getName();
        }
    }
}
