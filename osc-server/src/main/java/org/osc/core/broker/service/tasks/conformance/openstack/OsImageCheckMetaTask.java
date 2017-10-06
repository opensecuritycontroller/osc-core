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

import org.openstack4j.model.image.v2.Image;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4jGlance;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.util.log.LogProvider;
import org.osc.core.common.job.TaskGuard;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

import com.google.common.base.Joiner;

@Component(service = OsImageCheckMetaTask.class)
public class OsImageCheckMetaTask extends TransactionalMetaTask {
    private static final Logger LOG = LogProvider.getLogger(OsImageCheckMetaTask.class);

    @Reference
    DeleteImageReferenceTask deleteImageReferenceTask;

    @Reference
    DeleteImageFromGlanceTask deleteImageFromGlanceTask;

    @Reference
    UploadImageToGlanceTask uploadImageToGlanceTask;

    @Reference
    UpdateVsWithImageVersionTask updateVsWithImageVersionTask;

    private VirtualSystem vs;
    private String vcName;
    private String region;
    private Endpoint osEndPoint;
    private TaskGraph tg;
    private Openstack4jGlance glance;

    public OsImageCheckMetaTask() {
    }

    public OsImageCheckMetaTask create(VirtualSystem vs, String region, Endpoint osEndPoint) {
        OsImageCheckMetaTask task = new OsImageCheckMetaTask(vs, region, osEndPoint, null);
        task.deleteImageReferenceTask = this.deleteImageReferenceTask;
        task.deleteImageFromGlanceTask = this.deleteImageFromGlanceTask;
        task.uploadImageToGlanceTask = this.uploadImageToGlanceTask;
        task.updateVsWithImageVersionTask = this.updateVsWithImageVersionTask;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    // package private constructor for testing purposes
    OsImageCheckMetaTask(VirtualSystem vs, String region, Endpoint osEndPoint, Openstack4jGlance glance) {
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
            this.glance = new Openstack4jGlance(this.osEndPoint);
        }

        Set<OsImageReference> imageReferences = this.vs.getOsImageReference();
        boolean uploadImage = true;

        try {
            for (OsImageReference imageReference : imageReferences) {
                if (imageReference.getRegion().equals(this.region)) {
                    Image image = this.glance.getImageById(imageReference.getRegion(), imageReference.getImageRefId());
                    if (image == null || image.getStatus() != Image.ImageStatus.ACTIVE) {
                        this.tg.appendTask(this.deleteImageReferenceTask.create(imageReference, this.vs));
                    } else if (!image.getName().equals(expectedGlanceImageName)) {
                        // Assume image name is changed, means the version is upgraded since image name contains version
                        // information. Delete existing image and create new image.
                        this.tg.appendTask(this.deleteImageFromGlanceTask.create(this.region, imageReference, this.osEndPoint));
                    } else {
                        uploadImage = false;
                    }
                }
            }
        } finally {
            this.glance.close();
        }
        if (uploadImage) {
            this.tg.appendTask(this.uploadImageToGlanceTask.create(this.vs, this.region, expectedGlanceImageName,
                    applianceSoftwareVersion, this.osEndPoint), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        } else if (!imageReferences.isEmpty()) {
            this.tg.appendTask(this.updateVsWithImageVersionTask.create(this.vs));
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
