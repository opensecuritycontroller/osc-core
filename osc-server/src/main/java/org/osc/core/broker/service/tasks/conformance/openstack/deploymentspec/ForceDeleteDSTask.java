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
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

@Component(service = ForceDeleteDSTask.class)
public class ForceDeleteDSTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(ForceDeleteDSTask.class);

    private DeploymentSpec ds;

    public ForceDeleteDSTask create(DeploymentSpec ds) {
        ForceDeleteDSTask task = new ForceDeleteDSTask();
        task.ds = ds;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public String getName() {
        return String.format("Force Delete Deployment Specification '%s'", this.ds.getName());
    }

    @Override
    public void executeTransaction(EntityManager em) {
        log.info("Force Deleting Deployment Specification: " + this.ds.getName());
        // load deployment spec from database to avoid lazy loading issues
        this.ds = DeploymentSpecEntityMgr.findById(em, this.ds.getId());

        // remove DAI(s) for this ds
        for (DistributedApplianceInstance dai : this.ds.getDistributedApplianceInstances()) {
            dai.getProtectedPorts().clear();
            OSCEntityManager.delete(em, dai, this.txBroadcastUtil);
        }

        // remove the sg reference from database
        if (this.ds.getVirtualSystem().getVirtualizationConnector().getVirtualizationType().isOpenstack()) {
            boolean osSgCanBeDeleted = DeploymentSpecEntityMgr.findDeploymentSpecsByVirtualSystemProjectAndRegion(em,
                    this.ds.getVirtualSystem(), this.ds.getProjectId(), this.ds.getRegion()).size() <= 1;

            if (osSgCanBeDeleted) {
                OSCEntityManager.delete(em, this.ds.getOsSecurityGroupReference(), this.txBroadcastUtil);
            }
        }

        // delete DS from the database
        OSCEntityManager.delete(em, this.ds, this.txBroadcastUtil);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }

}
