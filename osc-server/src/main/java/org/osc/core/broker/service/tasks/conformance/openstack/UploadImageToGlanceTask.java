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

import java.io.File;
import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4jGlance;
import org.osc.core.broker.service.appliance.UploadConfig;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.slf4j.Logger;

@Component(service = UploadImageToGlanceTask.class,
        configurationPid = "org.osc.core.broker.upload",
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class UploadImageToGlanceTask extends TransactionalTask {

    private final Logger log = LoggerFactory.getLogger(UploadImageToGlanceTask.class);

    private String region;
    private VirtualSystem vs;
    private String glanceImageName;
    private ApplianceSoftwareVersion applianceSoftwareVersion;
    private Endpoint osEndPoint;
    private String uploadPath;

    @Activate
    void activate(UploadConfig config) {
        this.uploadPath = config.upload_path();
    }

    public UploadImageToGlanceTask create(VirtualSystem vs, String region, String glanceImageName, ApplianceSoftwareVersion applianceSoftwareVersion, Endpoint osEndPoint) {
        UploadImageToGlanceTask task = new UploadImageToGlanceTask();
        task.vs = vs;
        task.region = region;
        task.applianceSoftwareVersion = applianceSoftwareVersion;
        task.osEndPoint = osEndPoint;
        task.glanceImageName = glanceImageName;
        task.name = task.getName();
        task.uploadPath = this.uploadPath;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        OSCEntityManager<VirtualSystem> emgr = new OSCEntityManager<>(VirtualSystem.class, em, this.txBroadcastUtil);

        this.vs = emgr.findByPrimaryKey(this.vs.getId());

        this.log.info("Uploading image " + this.glanceImageName + " to region + " + this.region);
        File imageFile = new File(this.uploadPath + this.applianceSoftwareVersion.getImageUrl());
        try (Openstack4jGlance glance = new Openstack4jGlance(this.osEndPoint)) {
            String imageId = glance.uploadImage(this.region, this.glanceImageName, imageFile, this.applianceSoftwareVersion.getImageProperties());
            this.vs.addOsImageReference(new OsImageReference(this.vs, this.region, imageId));
        }
        OSCEntityManager.update(em, this.vs, this.txBroadcastUtil);
    }

    @Override
    public String getName() {
        return String.format("Uploading image '%s' to region '%s'", this.glanceImageName, this.region);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
