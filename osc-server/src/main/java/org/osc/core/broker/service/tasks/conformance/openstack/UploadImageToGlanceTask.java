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

import static org.jclouds.openstack.glance.v1_0.options.CreateImageOptions.Builder.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.jclouds.openstack.glance.v1_0.domain.ContainerFormat;
import org.jclouds.openstack.glance.v1_0.domain.DiskFormat;
import org.jclouds.openstack.glance.v1_0.options.CreateImageOptions;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudGlance;
import org.osc.core.broker.service.appliance.UploadConfig;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

@Component(service = UploadImageToGlanceTask.class,
        configurationPid = "org.osc.core.broker.upload",
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class UploadImageToGlanceTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(UploadImageToGlanceTask.class);

    private List<CreateImageOptions> imageOptions = new ArrayList<>(Arrays.asList(
            containerFormat(ContainerFormat.BARE), isPublic(false)));
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
        OSCEntityManager<VirtualSystem> emgr = new OSCEntityManager<VirtualSystem>(VirtualSystem.class, em, this.txBroadcastUtil);

        this.vs = emgr.findByPrimaryKey(this.vs.getId());

        JCloudGlance glance = new JCloudGlance(this.osEndPoint);
        try {
            this.log.info("Uploading image " + this.glanceImageName + " to region + " + this.region);
            File imageFile = new File(this.uploadPath + this.applianceSoftwareVersion.getImageUrl());
            String fileExtension = FilenameUtils.getExtension(this.applianceSoftwareVersion.getImageUrl())
                    .toUpperCase();
            if (fileExtension.equals("QCOW")) {
                this.imageOptions.add(diskFormat(DiskFormat.QCOW2));
            } else {
                DiskFormat diskFormat = DiskFormat.fromValue(fileExtension);
                if (diskFormat == DiskFormat.UNRECOGNIZED) {
                    throw new VmidcBrokerValidationException("Unsupported Disk Image format: '" + fileExtension + "'.");
                }
                this.imageOptions.add(diskFormat(diskFormat));
            }
            if (this.applianceSoftwareVersion.getImageProperties() != null) {
                for (Entry<String, String> entry : this.applianceSoftwareVersion.getImageProperties().entrySet()) {
                    this.imageOptions.add(property(entry.getKey(), entry.getValue()));
                }
            }

            String imageId = glance.uploadImage(this.region, this.glanceImageName, imageFile,
                    this.imageOptions.toArray(new CreateImageOptions[] {}));

            this.vs.addOsImageReference(new OsImageReference(this.vs, this.region, imageId));

            OSCEntityManager.update(em, this.vs, this.txBroadcastUtil);
        } finally {
            glance.close();
        }
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
