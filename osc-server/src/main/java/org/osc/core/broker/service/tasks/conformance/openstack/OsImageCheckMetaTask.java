/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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

import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.jclouds.openstack.glance.v1_0.domain.Image.Status;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudGlance;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.jclouds.openstack.glance.v1_0.domain.ImageDetails;

import com.google.common.base.Joiner;

public class OsImageCheckMetaTask extends TransactionalMetaTask {

    private static final Logger log = Logger.getLogger(OsImageCheckMetaTask.class);

    private VirtualSystem vs;
    private String vcName;
    private String region;
    private Endpoint osEndPoint;
    private TaskGraph tg;

    public OsImageCheckMetaTask(VirtualSystem vs, String region, Endpoint osEndPoint) {
        this.vs = vs;
        this.vcName = vs.getVirtualizationConnector().getName();
        this.osEndPoint = osEndPoint;
        this.region = region;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.tg = new TaskGraph();


        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId(),
                new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));

        log.info("Checking VS" + this.vs.getName() + " has the corresponding image uploaded in glance");

        ApplianceSoftwareVersion applianceSoftwareVersion = this.vs.getApplianceSoftwareVersion();

        String expectedGlanceImageName = Joiner.on("-").join(applianceSoftwareVersion.getAppliance().getModel(),
                applianceSoftwareVersion.getApplianceSoftwareVersion(), this.vs.getName(),
                applianceSoftwareVersion.getImageUrl());

        JCloudGlance glance = new JCloudGlance(this.osEndPoint);
        try {
            Set<OsImageReference> imageReferences = this.vs.getOsImageReference();
            boolean uploadImage = true;

            for (Iterator<OsImageReference> iterator = imageReferences.iterator(); iterator.hasNext();) {
                OsImageReference imageReference = iterator.next();
                if (imageReference.getRegion().equals(this.region)) {
                    ImageDetails image = glance.getImageById(imageReference.getRegion(), imageReference.getImageRefId());
                    if (image == null || image != null && image.getStatus() != Status.ACTIVE) {
                        iterator.remove();
                        EntityManager.delete(session, imageReference);
                    } else if (!image.getName().equals(expectedGlanceImageName)) {
                        // Assume image name is changed, means the version is upgraded since image name contains version
                        // information. Delete existing image and create new image.
                        this.tg.addTask(new DeleteImageFromGlanceTask(this.region, imageReference, this.osEndPoint));
                    } else {
                        uploadImage = false;
                        // For any existing images in db which dont have the version set, set the version
                        if(imageReference.getApplianceVersion() == null) {
                            imageReference.setApplianceVersion(this.vs.getApplianceSoftwareVersion());
                        }
                    }
                }
            }
            if (uploadImage) {
                this.tg.appendTask(new UploadImageToGlanceTask(this.vs, this.region, expectedGlanceImageName,
                        applianceSoftwareVersion, this.osEndPoint), TaskGuard.ALL_PREDECESSORS_COMPLETED);
            }
            EntityManager.update(session, this.vs);
        } finally {
            glance.close();
        }
    }

    @Override
    public String getName() {
        return String.format("Checking Image exists in Glance for Virtual Connector '%s' in Region '%s'", this.vcName,
                this.region);
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
