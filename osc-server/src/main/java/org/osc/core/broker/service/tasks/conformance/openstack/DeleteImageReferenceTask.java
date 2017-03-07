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

import org.apache.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class DeleteImageReferenceTask  extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(DeleteImageReferenceTask.class);

    private OsImageReference imageReference;
    private VirtualSystem vs;

    public DeleteImageReferenceTask(OsImageReference imageReference, VirtualSystem vs) {
        this.imageReference = imageReference;
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.imageReference = session.get(OsImageReference.class, this.imageReference.getId());
        this.vs = session.get(VirtualSystem.class, this.vs.getId(),
                new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));

        LOG.info("Deleting image " + this.imageReference.getImageRefId() + " from DB");

        EntityManager.delete(session, this.imageReference);

        this.vs.removeOsImageReference(this.imageReference);

        LOG.info("Updating virtual system " + this.vs.getName());

        EntityManager.update(session, this.vs);
    }

    @Override
    public String getName() {
        return "Deleting image with id " + this.imageReference.getImageRefId();
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }
}