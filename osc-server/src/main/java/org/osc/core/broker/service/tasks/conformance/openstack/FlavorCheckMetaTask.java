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

import com.google.common.base.Joiner;
import org.apache.log4j.Logger;
import org.openstack4j.model.compute.Flavor;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.OsFlavorReference;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNova;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import java.util.Iterator;
import java.util.Set;

@Component(service = FlavorCheckMetaTask.class)
public class FlavorCheckMetaTask extends TransactionalMetaTask {

    private static final Logger log = Logger.getLogger(FlavorCheckMetaTask.class);

    @Reference
    CreateFlavorTask createFlavorTask;

    @Reference
    DeleteFlavorTask deleteFlavor;

    private VirtualSystem vs;
    private String vcName;
    private String region;
    private Endpoint osEndPoint;
    private TaskGraph tg;

    public FlavorCheckMetaTask create(VirtualSystem vs, String region, Endpoint osEndPoint) {
        FlavorCheckMetaTask task = new FlavorCheckMetaTask();
        task.vs = vs;
        task.vcName = vs.getVirtualizationConnector().getName();
        task.osEndPoint = osEndPoint;
        task.region = region;
        task.name = task.getName();
        task.createFlavorTask = this.createFlavorTask;
        task.deleteFlavor = this.deleteFlavor;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();

        VirtualSystem vs = em.find(VirtualSystem.class, this.vs.getId(),
                LockModeType.PESSIMISTIC_WRITE);

        log.info("Checking VS" + vs.getName() + " has the corresponding flavor");

        ApplianceSoftwareVersion applianceSoftwareVersion = vs.getApplianceSoftwareVersion();

        String expectedFlavorName = Joiner.on("-").join(applianceSoftwareVersion.getAppliance().getModel(),
                applianceSoftwareVersion.getApplianceSoftwareVersion(), vs.getName(),
                applianceSoftwareVersion.getImageUrl());

        try (Openstack4JNova nova = new Openstack4JNova(this.osEndPoint)) {
            Set<OsFlavorReference> flavorReferences = vs.getOsFlavorReference();
            boolean createFlavor = true;

            for (Iterator<OsFlavorReference> iterator = flavorReferences.iterator(); iterator.hasNext(); ) {
                OsFlavorReference flavorReference = iterator.next();
                if (flavorReference.getRegion().equals(this.region)) {
                    Flavor flavor = nova.getFlavorById(flavorReference.getRegion(), flavorReference.getFlavorRefId());
                    if (flavor == null) {
                        iterator.remove();
                        OSCEntityManager.delete(em, flavorReference, this.txBroadcastUtil);
                    } else if (!flavor.getName().equals(expectedFlavorName)) {
                        // Assume flavor name is changed, means the version is upgraded since flavor name contains version
                        // information. Delete existing flavor and create new flavor.
                        this.tg.addTask(this.deleteFlavor.create(this.region, flavorReference, this.osEndPoint));
                    } else {
                        createFlavor = false;
                    }
                }
            }
            if (createFlavor) {
                this.tg.appendTask(this.createFlavorTask.create(vs, this.region, expectedFlavorName, applianceSoftwareVersion,
                        this.osEndPoint));
            }
        }

        OSCEntityManager.update(em, vs, this.txBroadcastUtil);
    }

    @Override
    public String getName() {
        return String.format("Checking Flavor exists for Virtual Connector '%s' in Region '%s'", this.vcName, this.region);
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
