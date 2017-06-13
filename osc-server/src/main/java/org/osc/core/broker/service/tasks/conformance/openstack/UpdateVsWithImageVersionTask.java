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
import javax.persistence.LockModeType;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

@Component(service = UpdateVsWithImageVersionTask.class)
public class UpdateVsWithImageVersionTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(UpdateVsWithImageVersionTask.class);

    private VirtualSystem vs;

    public UpdateVsWithImageVersionTask create(VirtualSystem vs) {
        UpdateVsWithImageVersionTask task = new UpdateVsWithImageVersionTask();
        task.vs = vs;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.vs = em.find(VirtualSystem.class, this.vs.getId(),
                LockModeType.PESSIMISTIC_WRITE);

        LOG.info("Updating image references to virtual system " + this.vs.getName());

        Set<OsImageReference> imageReferences = this.vs.getOsImageReference();

        boolean needsUpdate = false;

        // For any existing images in db which dont have the version set, set the version
        for (OsImageReference imageReference : imageReferences) {
            if(imageReference.getApplianceVersion() == null) {
                imageReference.setApplianceVersion(this.vs.getApplianceSoftwareVersion());
                needsUpdate = true;
            }
        }

        if(needsUpdate) {
            OSCEntityManager.update(em, this.vs, this.txBroadcastUtil);
            LOG.info("Updating virtual system " + this.vs.getName());
        }
    }

    @Override
    public String getName() {
        return "Updating virtual system " + this.vs.getName();
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }
}