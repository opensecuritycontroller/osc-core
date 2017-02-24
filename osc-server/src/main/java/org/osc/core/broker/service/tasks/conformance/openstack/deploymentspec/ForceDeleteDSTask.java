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
package org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class ForceDeleteDSTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(ForceDeleteDSTask.class);

    private DeploymentSpec ds;

    public ForceDeleteDSTask(DeploymentSpec ds) {
        this.ds = ds;
        this.name = getName();
    }

    @Override
    public String getName() {
        return String.format("Force Delete Deployment Specification '%s'", this.ds.getName());
    }

    @Override
    public void executeTransaction(Session session) {
        log.info("Force Deleting Deployment Specification: " + this.ds.getName());
        // load deployment spec from database to avoid lazy loading issues
        this.ds = DeploymentSpecEntityMgr.findById(session, this.ds.getId());

        // remove DAI(s) for this ds
        for (DistributedApplianceInstance dai : this.ds.getDistributedApplianceInstances()) {
            for (VMPort port : dai.getProtectedPorts()) {
                dai.removeProtectedPort(port);
            }
            EntityManager.delete(session, dai);
        }

        // remove the sg reference from database
        boolean osSgCanBeDeleted = DeploymentSpecEntityMgr.findDeploymentSpecsByVirtualSystemTenantAndRegion(session,
                this.ds.getVirtualSystem(), this.ds.getTenantId(), this.ds.getRegion()).size() <= 1;

        if (osSgCanBeDeleted) {
            EntityManager.delete(session, this.ds.getOsSecurityGroupReference());
        }

        // delete DS from the database
        EntityManager.delete(session, this.ds);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }

}
