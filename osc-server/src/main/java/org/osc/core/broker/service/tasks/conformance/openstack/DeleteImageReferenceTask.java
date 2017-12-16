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

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;

@Component(service = DeleteImageReferenceTask.class)
public class DeleteImageReferenceTask  extends TransactionalTask {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteImageReferenceTask.class);

    private OsImageReference imageReference;
    private VirtualSystem vs;

    public DeleteImageReferenceTask create(OsImageReference imageReference, VirtualSystem vs) {
        DeleteImageReferenceTask task = new DeleteImageReferenceTask();
        task.imageReference = imageReference;
        task.vs = vs;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.imageReference = em.find(OsImageReference.class, this.imageReference.getId());
        this.vs = em.find(VirtualSystem.class, this.vs.getId(),
                LockModeType.PESSIMISTIC_WRITE);

        LOG.info("Deleting image " + this.imageReference.getImageRefId() + " from DB");

        OSCEntityManager.delete(em, this.imageReference, this.txBroadcastUtil);

        this.vs.removeOsImageReference(this.imageReference);

        LOG.info("Updating virtual system " + this.vs.getName());

        OSCEntityManager.update(em, this.vs, this.txBroadcastUtil);
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
