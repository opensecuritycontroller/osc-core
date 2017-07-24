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

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.service.persistence.DistributedApplianceEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
@Component(service = ForceDeleteDATask.class)
public class ForceDeleteDATask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(ForceDeleteDATask.class);

    private DistributedAppliance da;

    @Reference
    private ForceDeleteVirtualSystemTask forceDeleteVsTask;

    public ForceDeleteDATask create(DistributedAppliance da) {
        ForceDeleteDATask task = new ForceDeleteDATask();
        task.da = da;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;
        task.forceDeleteVsTask = this.forceDeleteVsTask;

        return task;
    }

    @Override
    public String getName() {
        return "Force Delete Distributed Appliance '" + this.da.getName() + "'";
    }

    @Override
    public void executeTransaction(EntityManager em) {
        log.debug("Force Delete Task for DA: " + this.da.getId());
        // load Distributed Appliance from Database
        DistributedAppliance da = DistributedApplianceEntityMgr.findById(em, this.da.getId());

        // remove all virtual systems for this DA
        for (VirtualSystem vs : da.getVirtualSystems()) {
            this.forceDeleteVsTask.create(vs).executeTransaction(em);
        }

        // delete distributed appliance from database
        OSCEntityManager.delete(em, da, this.txBroadcastUtil);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.da);
    }

}
