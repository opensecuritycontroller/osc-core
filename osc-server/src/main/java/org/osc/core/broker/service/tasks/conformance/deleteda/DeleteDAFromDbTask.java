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
package org.osc.core.broker.service.tasks.conformance.deleteda;

import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;

@Component(service=DeleteDAFromDbTask.class)
public class DeleteDAFromDbTask extends TransactionalTask {
    private static final Logger log = LoggerFactory.getLogger(DeleteDAFromDbTask.class);

    private DistributedAppliance da;

    public DeleteDAFromDbTask create(DistributedAppliance da) {
        DeleteDAFromDbTask task = new DeleteDAFromDbTask();
        task.da = da;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public String getName() {
        return "Delete Distributed Appliance '" + this.da.getName() + "'";
    }

    @Override
    public void executeTransaction(EntityManager em) {
        log.debug("Start Executing DeleteDAFromDb Task for DA: " + this.da.getId());
        this.da = em.find(DistributedAppliance.class, this.da.getId());
        OSCEntityManager.delete(em, this.da, this.txBroadcastUtil);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.da);
    }

}
