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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;

import com.google.common.base.Joiner;

public class OsImageCheckMetaTaskTestData {
    public static List<VirtualSystem> TEST_VIRTUAL_SYSTEMS = new ArrayList<VirtualSystem>();

    public static String REGION = "region";
    public static String REGION_ONE = "region_1";
    public static String REGION_TWO = "region_2";
    public static String REGION_THREE = "region_3";
    public static String REGION_FOUR = "region_4";
    public static String REGION_FIVE = "region_5";
    public static String UNEXPECTED_REGION = "unexpected_region";

    public static String SINGLE_REF_ID = "ref_id";
    public static String REF_ID_ONE = "ref_id_1";
    public static String REF_ID_TWO = "ref_id_2";
    public static String REF_ID_THREE = "ref_id_3";

    public static VirtualSystem VS_WITHOUT_IMAGE_REFERENCE = createVirtualSystem(1L);
    public static VirtualSystem VS_WITH_NULL_IMAGE = createVirtualSystemWithImageReference(2L, REGION_ONE, SINGLE_REF_ID);
    public static VirtualSystem VS_WITH_INACTIVE_IMAGE_STATUS = createVirtualSystemWithImageReference(3L, REGION_TWO, SINGLE_REF_ID);
    public static VirtualSystem VS_WITH_UNEXPECTED_IMAGE_NAME = createVirtualSystemWithImageReference(4L, REGION_THREE, SINGLE_REF_ID);
    public static VirtualSystem VS_WITH_UNEXPECTED_REGION = createVirtualSystemWithImageReference(5L, REGION_FOUR, SINGLE_REF_ID);

    public static VirtualSystem VS_WITH_MULTIPLE_IMAGES = createVirtualSystemWithMultipleImageReferences(6L);

    public static TaskGraph emptyGraph(VirtualSystem vs) {
        TaskGraph expectedGraph = new TaskGraph();

        return expectedGraph;
    }

    public static TaskGraph deleteImageFromDBAndUploadToGlanceGraph(VirtualSystem vs) {
        ApplianceSoftwareVersion applianceSoftwareVersion = vs.getApplianceSoftwareVersion();
        String expectedGlanceImageName = Joiner.on("-").join(applianceSoftwareVersion.getAppliance().getModel(),
                applianceSoftwareVersion.getApplianceSoftwareVersion(), vs.getName(), applianceSoftwareVersion.getImageUrl());

        TaskGraph expectedGraph = new TaskGraph();

        String region = "temp_region";
        Set<OsImageReference> imageReferences = vs.getOsImageReference();
        for(OsImageReference imageReference: imageReferences) {
            region = imageReference.getRegion();
            expectedGraph.appendTask(new DeleteImageReferenceTask(imageReference, vs));
        }
        expectedGraph.appendTask(new UploadImageToGlanceTask(vs, region, expectedGlanceImageName, applianceSoftwareVersion, null),
                TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    public static TaskGraph deleteImageFromGlanceAndUploadToGlanceGraph(VirtualSystem vs) {
        ApplianceSoftwareVersion applianceSoftwareVersion = vs.getApplianceSoftwareVersion();
        String expectedGlanceImageName = Joiner.on("-").join(applianceSoftwareVersion.getAppliance().getModel(),
                applianceSoftwareVersion.getApplianceSoftwareVersion(), vs.getName(), applianceSoftwareVersion.getImageUrl());

        TaskGraph expectedGraph = new TaskGraph();

        String region = "temp_region";
        Set<OsImageReference> imageReferences = vs.getOsImageReference();
        for(OsImageReference imageReference: imageReferences) {
            region = imageReference.getRegion();
            expectedGraph.appendTask(new DeleteImageFromGlanceTask(imageReference.getRegion(), imageReference, null));
        }
        expectedGraph.appendTask(new UploadImageToGlanceTask(vs, region,
                expectedGlanceImageName, applianceSoftwareVersion, null), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    public static TaskGraph deleteImagesAndUploadToGlance(VirtualSystem vs) {
        ApplianceSoftwareVersion applianceSoftwareVersion = vs.getApplianceSoftwareVersion();
        String expectedGlanceImageName = Joiner.on("-").join(applianceSoftwareVersion.getAppliance().getModel(),
                applianceSoftwareVersion.getApplianceSoftwareVersion(), vs.getName(), applianceSoftwareVersion.getImageUrl());

        TaskGraph expectedGraph = new TaskGraph();

        String region = REGION_FIVE;
        Set<OsImageReference> imageReferences = vs.getOsImageReference();
        for(OsImageReference imageReference: imageReferences) {
            if(imageReference.getImageRefId().equals(REF_ID_TWO)) {
                expectedGraph.appendTask(new DeleteImageFromGlanceTask(imageReference.getRegion(), imageReference, null));
            } else {
                expectedGraph.appendTask(new DeleteImageReferenceTask(imageReference, vs));
            }
        }
        expectedGraph.appendTask(new UploadImageToGlanceTask(vs, region,
                expectedGlanceImageName, applianceSoftwareVersion, null), TaskGuard.ALL_PREDECESSORS_COMPLETED);

        return expectedGraph;
    }

    public static TaskGraph updateVSWithImageVersionGraph(VirtualSystem vs) {
        TaskGraph expectedGraph = new TaskGraph();

        expectedGraph.appendTask(new UpdateVsWithImageVersionTask(vs));

        return expectedGraph;
    }

    private static VirtualSystem createVirtualSystem(Long vsId) {
        Appliance appliance = new Appliance();
        appliance.setModel("model");
        ApplianceSoftwareVersion applianceSoftwareVersion = new ApplianceSoftwareVersion(appliance);
        applianceSoftwareVersion.setImageUrl("imageUrl");
        applianceSoftwareVersion.setApplianceSoftwareVersion("applianceSoftwareVersion");

        VirtualizationConnector vc = new VirtualizationConnector();
        vc.setName("vc_name");

        VirtualSystem vs = new VirtualSystem();
        vs.setId(vsId);
        vs.setName("vs_name");
        vs.setVirtualizationConnector(vc);
        vs.setApplianceSoftwareVersion(applianceSoftwareVersion);

        TEST_VIRTUAL_SYSTEMS.add(vs);

        return vs;
    }

    private static VirtualSystem createVirtualSystemWithImageReference(Long vsId, String region, String refId) {
        VirtualSystem vs = createVirtualSystem(vsId);
        createImageReference(vs, region, refId);

        return vs;
    }

    private static VirtualSystem createVirtualSystemWithMultipleImageReferences(Long vsId) {
        VirtualSystem vs = createVirtualSystem(vsId);

        createImageReference(vs, REGION_FIVE, REF_ID_ONE);
        createImageReference(vs, REGION_FIVE, REF_ID_TWO);
        createImageReference(vs, REGION_FIVE, REF_ID_THREE);

        return vs;
    }

    private static void createImageReference(VirtualSystem vs, String region, String refId) {
        OsImageReference imageReference = new OsImageReference(vs, region, refId);
        vs.addOsImageReference(imageReference);
    }
}
