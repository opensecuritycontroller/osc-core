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
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.OpenstackUtil;

public class DeleteDAIFromDbTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(DeleteDAIFromDbTask.class);

    private DistributedApplianceInstance dai;

    public DeleteDAIFromDbTask(DistributedApplianceInstance dai) {
        this.dai = dai;
        this.name = getName();
    }

    @Override
    public String getName() {
        return "Delete Distributed Appliance Instance '" + this.dai.getName() + "'";
    }

    @Override
    public void executeTransaction(EntityManager em) throws VmidcException, InterruptedException {
        log.debug("Start Executing DeleteDAIFromDb Task : " + this.dai.getId());
        this.dai = em.find(DistributedApplianceInstance.class, this.dai.getId());

        OpenstackUtil.scheduleSecurityGroupJobsRelatedToDai(em, this.dai, this);
        OSCEntityManager.delete(em, this.dai);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
