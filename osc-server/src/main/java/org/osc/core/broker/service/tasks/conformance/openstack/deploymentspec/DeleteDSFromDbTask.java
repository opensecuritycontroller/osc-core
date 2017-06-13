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
package org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec;

import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

@Component(service=DeleteDSFromDbTask.class)
public class DeleteDSFromDbTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(DeleteDSFromDbTask.class);

    private DeploymentSpec ds;

    public DeleteDSFromDbTask create(DeploymentSpec ds) {
        DeleteDSFromDbTask task = new DeleteDSFromDbTask();
        task.ds = ds;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public String getName() {
        return String.format("Delete Deployment Specification '%s'", this.ds.getName());
    }

    @Override
    public void executeTransaction(EntityManager em) {
        log.debug("Start Executing DeleteDSFromDb Task : " + this.ds.getId());
        this.ds = em.find(DeploymentSpec.class, this.ds.getId());
        OSCEntityManager.delete(em, this.ds, this.txBroadcastUtil);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }

}
