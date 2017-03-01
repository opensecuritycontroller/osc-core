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

import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.FlavorCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.OsImageCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.OsSecurityGroupCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.ValidateDSNetworkTask.NetworkType;

/**
 * Conforms the DS according to its settings. This task assumes a Lock has been placed on the DS by the job
 * containing this task
 */
public class DSConformanceCheckMetaTask extends TransactionalMetaTask {

    private DeploymentSpec ds;
    private final Endpoint endPoint;
    private TaskGraph tg;

    public DSConformanceCheckMetaTask(DeploymentSpec ds, Endpoint endPoint) {
        this.ds = ds;
        this.endPoint = endPoint;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.tg = new TaskGraph();

        EntityManager<DeploymentSpec> dsEmgr = new EntityManager<DeploymentSpec>(DeploymentSpec.class, session);
        this.ds = dsEmgr.findByPrimaryKey(this.ds.getId());

        VirtualSystem virtualSystem = this.ds.getVirtualSystem();

        OsImageCheckMetaTask imageCheckTask = new OsImageCheckMetaTask(virtualSystem, this.ds.getRegion(),
                this.endPoint);
        FlavorCheckMetaTask flavorCheckTask = new FlavorCheckMetaTask(virtualSystem, this.ds.getRegion(), this.endPoint);
        OsSecurityGroupCheckMetaTask osSecurityGroupCheckMetaTask = new OsSecurityGroupCheckMetaTask(this.ds);

        this.tg.appendTask(new ValidateDSTenantTask(this.ds));
        this.tg.appendTask(new ValidateDSNetworkTask(this.ds, this.endPoint, NetworkType.MANAGEMENT));
        this.tg.appendTask(new ValidateDSNetworkTask(this.ds, this.endPoint, NetworkType.INSPECTION));
        // if DS is already marked for deletion don't upload the image
        if (!this.ds.getMarkedForDeletion() && !virtualSystem.getMarkedForDeletion()
                && !virtualSystem.getDistributedAppliance().getMarkedForDeletion()) {
            this.tg.addTask(flavorCheckTask);
            this.tg.appendTask(imageCheckTask);
            this.tg.appendTask(osSecurityGroupCheckMetaTask);
        }
        this.tg.appendTask(new DSUpdateOrDeleteMetaTask(this.ds, this.endPoint));
    }

    @Override
    public String getName() {
        return String.format("Checking Deployment Spec '%s'", this.ds.getName());
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }

}