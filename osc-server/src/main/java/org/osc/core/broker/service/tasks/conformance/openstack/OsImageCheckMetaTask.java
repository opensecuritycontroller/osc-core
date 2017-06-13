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

import org.apache.log4j.Logger;
import org.jclouds.openstack.glance.v1_0.domain.Image.Status;
import org.jclouds.openstack.glance.v1_0.domain.ImageDetails;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudGlance;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;

import com.google.common.base.Joiner;

public class OsImageCheckMetaTask extends TransactionalMetaTask {
    private static final Logger LOG = Logger.getLogger(OsImageCheckMetaTask.class);

    private VirtualSystem vs;
    private String vcName;
    private String region;
    private Endpoint osEndPoint;
    private TaskGraph tg;
    private JCloudGlance glance;

    public OsImageCheckMetaTask(VirtualSystem vs, String region, Endpoint osEndPoint) {
        this(vs, region, osEndPoint, null);
    }

    // package private constructor for testing purposes
    OsImageCheckMetaTask(VirtualSystem vs, String region, Endpoint osEndPoint, JCloudGlance glance) {
        this.vs = vs;
        this.vcName = vs.getVirtualizationConnector().getName();
        this.osEndPoint = osEndPoint;
        this.region = region;
        this.name = getName();
        this.glance = glance;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();

        this.vs = em.find(VirtualSystem.class, this.vs.getId());

        LOG.info("Checking VS " + this.vs.getName() + " has the corresponding image uploaded in glance");

        ApplianceSoftwareVersion applianceSoftwareVersion = this.vs.getApplianceSoftwareVersion();

        String expectedGlanceImageName = Joiner.on("-").join(applianceSoftwareVersion.getAppliance().getModel(),
                applianceSoftwareVersion.getApplianceSoftwareVersion(), this.vs.getName(),
                applianceSoftwareVersion.getImageUrl());

        if (this.glance == null) {
            this.glance = new JCloudGlance(this.osEndPoint);
        }

        try {
            Set<OsImageReference> imageReferences = this.vs.getOsImageReference();
            boolean uploadImage = true;

            for (OsImageReference imageReference : imageReferences) {
                if (imageReference.getRegion().equals(this.region)) {
                    ImageDetails image = this.glance.getImageById(imageReference.getRegion(), imageReference.getImageRefId());
                    if (image == null || image != null && image.getStatus() != Status.ACTIVE) {
                        this.tg.appendTask(new DeleteImageReferenceTask(imageReference, this.vs));
                    } else if (!image.getName().equals(expectedGlanceImageName)) {
                        // Assume image name is changed, means the version is upgraded since image name contains version
                        // information. Delete existing image and create new image.
                        this.tg.appendTask(new DeleteImageFromGlanceTask(this.region, imageReference, this.osEndPoint));
                    } else {
                        uploadImage = false;
                    }
                }
            }
            if (uploadImage) {
                this.tg.appendTask(new UploadImageToGlanceTask(this.vs, this.region, expectedGlanceImageName,
                        applianceSoftwareVersion, this.osEndPoint), TaskGuard.ALL_PREDECESSORS_COMPLETED);
            } else if (!imageReferences.isEmpty()){
                this.tg.appendTask(new UpdateVsWithImageVersionTask(this.vs));
            }
        } finally {
            this.glance.close();
        }
    }

    @Override
    public String getName() {
        return "Checking Image exists in Glance for Virtual Connector '" + this.vcName + "' in Region '" + this.region + "'";
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
