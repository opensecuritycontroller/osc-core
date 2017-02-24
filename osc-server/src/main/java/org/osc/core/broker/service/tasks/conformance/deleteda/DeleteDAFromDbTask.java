/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class DeleteDAFromDbTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(DeleteDAFromDbTask.class);

    private DistributedAppliance da;

    public DeleteDAFromDbTask(DistributedAppliance da) {
        this.da = da;
        this.name = getName();
    }

    @Override
    public String getName() {
        return "Delete Distributed Appliance '" + da.getName() + "'";
    }

    @Override
    public void executeTransaction(Session session) {
        log.debug("Start Executing DeleteDAFromDb Task for DA: " + da.getId());
        da = (DistributedAppliance) session.get(DistributedAppliance.class, da.getId());
        EntityManager.delete(session, da);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.da);
    }

}
