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

import org.apache.log4j.Logger;
import org.openstack4j.model.identity.v3.Project;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4jKeystone;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

import javax.persistence.EntityManager;
import java.util.Set;

/**
 * Validates the DS tenant exists and syncs the name if needed
 */
@Component(service = ValidateDSTenantTask.class)
public class ValidateDSTenantTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(ValidateDSTenantTask.class);

    private DeploymentSpec ds;

    public ValidateDSTenantTask create(DeploymentSpec ds) {
        ValidateDSTenantTask task = new ValidateDSTenantTask();
        task.ds = ds;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        OSCEntityManager<DeploymentSpec> dsEmgr = new OSCEntityManager<>(DeploymentSpec.class, em, this.txBroadcastUtil);
        this.ds = dsEmgr.findByPrimaryKey(this.ds.getId());

        if (!this.ds.getMarkedForDeletion()) {
            VirtualizationConnector vc = this.ds.getVirtualSystem().getVirtualizationConnector();
            this.log.info("Validating the DS tenant " + this.ds.getTenantName() + " exists.");

            try (Openstack4jKeystone keystone = new Openstack4jKeystone(new Endpoint(vc))) {
                Project tenant = keystone.getProjectById(this.ds.getTenantId());
                if (tenant == null) {
                    this.log.info("DS tenant " + this.ds.getTenantName() + " Deleted from openstack. Marking DS for deletion.");
                    // Tenant was deleted, mark ds for deleting as well
                    OSCEntityManager.markDeleted(em, this.ds, this.txBroadcastUtil);
                } else {
                    // Sync the tenant name if needed
                    if (!tenant.getName().equals(this.ds.getTenantName())) {
                        this.log.info("DS tenant name updated from " + this.ds.getTenantName() + " to " + tenant.getName());
                        this.ds.setTenantName(tenant.getName());
                        OSCEntityManager.update(em, this.ds, this.txBroadcastUtil);
                    }
                }
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Validating Deployment Specification '%s' for tenant '%s'", this.ds.getName(),
                this.ds.getTenantName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }

}
