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

import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.jclouds.openstack.nova.v2_0.domain.Flavor;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.OsFlavorReference;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;

import com.google.common.base.Joiner;

public class FlavorCheckMetaTask extends TransactionalMetaTask {

    private static final Logger log = Logger.getLogger(FlavorCheckMetaTask.class);

    private VirtualSystem vs;
    private String vcName;
    private String region;
    private Endpoint osEndPoint;
    private TaskGraph tg;

    public FlavorCheckMetaTask(VirtualSystem vs, String region, Endpoint osEndPoint) {
        this.vs = vs;
        this.vcName = vs.getVirtualizationConnector().getName();
        this.osEndPoint = osEndPoint;
        this.region = region;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.tg = new TaskGraph();

        VirtualSystem vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId(),
                new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));

        log.info("Checking VS" + vs.getName() + " has the corresponding flavor");

        ApplianceSoftwareVersion applianceSoftwareVersion = vs.getApplianceSoftwareVersion();

        String expectedFlavorName = Joiner.on("-").join(applianceSoftwareVersion.getAppliance().getModel(),
                applianceSoftwareVersion.getApplianceSoftwareVersion(), vs.getName(),
                applianceSoftwareVersion.getImageUrl());

        JCloudNova nova = new JCloudNova(this.osEndPoint);
        try {
            Set<OsFlavorReference> flavorReferences = vs.getOsFlavorReference();

            boolean createFlavor = true;

            for (Iterator<OsFlavorReference> iterator = flavorReferences.iterator(); iterator.hasNext();) {
                OsFlavorReference flavorReference = iterator.next();
                if (flavorReference.getRegion().equals(this.region)) {
                    Flavor flavor = nova.getFlavorById(flavorReference.getRegion(), flavorReference.getFlavorRefId());
                    if (flavor == null) {
                        iterator.remove();
                        EntityManager.delete(session, flavorReference);
                    } else if(!flavor.getName().equals(expectedFlavorName)) {
                        // Assume flavor name is changed, means the version is upgraded since flavor name contains version
                        // information. Delete existing flavor and create new flavor.
                        this.tg.addTask(new DeleteFlavorTask(this.region, flavorReference, this.osEndPoint));
                    } else {
                        createFlavor = false;
                    }
                }
            }
            if (createFlavor) {
                this.tg.appendTask(new CreateFlavorTask(vs, this.region, expectedFlavorName, applianceSoftwareVersion,
                        this.osEndPoint));
            }

            EntityManager.update(session, vs);
        } finally {
            nova.close();
        }
    }

    @Override
    public String getName() {
        return String.format("Checking Flavor exists for Virtual Connector '%s' in Region '%s'", this.vcName,
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
