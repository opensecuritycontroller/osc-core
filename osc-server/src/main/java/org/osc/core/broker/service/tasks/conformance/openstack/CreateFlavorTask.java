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

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.OsFlavorReference;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNova;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;

@Component(service = CreateFlavorTask.class)
public class CreateFlavorTask extends TransactionalTask {

    private final Logger log = LoggerFactory.getLogger(CreateFlavorTask.class);

    private String region;
    private VirtualSystem vs;
    private String flavorName;
    private ApplianceSoftwareVersion applianceSoftwareVersion;
    private Endpoint osEndPoint;

    public CreateFlavorTask create(VirtualSystem vs, String region, String flavorName, ApplianceSoftwareVersion applianceSoftwareVersion, Endpoint osEndPoint) {
        CreateFlavorTask task = new CreateFlavorTask();
        task.vs = vs;
        task.region = region;
        task.applianceSoftwareVersion = applianceSoftwareVersion;
        task.flavorName = flavorName;
        task.osEndPoint = osEndPoint;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        OSCEntityManager<VirtualSystem> vsEntityMgr = new OSCEntityManager<VirtualSystem>(VirtualSystem.class, em, this.txBroadcastUtil);
        OSCEntityManager<ApplianceSoftwareVersion> asvEntiyMgr = new OSCEntityManager<ApplianceSoftwareVersion>(ApplianceSoftwareVersion.class, em, this.txBroadcastUtil);

        this.vs = vsEntityMgr.findByPrimaryKey(this.vs.getId());
        this.applianceSoftwareVersion = asvEntiyMgr.findByPrimaryKey(this.applianceSoftwareVersion.getId());

        this.log.info("Creating flavor " + this.flavorName + " in region + " + this.region);
        String newFlavorId = this.vs.getName() + "_" + this.region;

        try (Openstack4JNova nova = new Openstack4JNova(this.osEndPoint)) {
            String flavorId = nova.createFlavor(this.region, newFlavorId, this.flavorName,
                    this.applianceSoftwareVersion.getDiskSizeInGb(), this.applianceSoftwareVersion.getMemoryInMb(),
                    this.applianceSoftwareVersion.getMinCpus());
            this.vs.addOsFlavorReference(new OsFlavorReference(this.vs, this.region, flavorId));
        }


        OSCEntityManager.update(em, this.vs, this.txBroadcastUtil);
    }

    @Override
    public String getName() {
        return String.format("Creating flavor '%s' in region '%s'", this.flavorName, this.region);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
