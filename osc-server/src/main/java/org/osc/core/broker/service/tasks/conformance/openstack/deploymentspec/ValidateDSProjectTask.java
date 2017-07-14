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
import org.openstack4j.model.identity.v3.Project;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4jKeystone;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

/**
 * Validates the DS project exists and syncs the name if needed
 */
@Component(service=ValidateDSProjectTask.class)
public class ValidateDSProjectTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(ValidateDSProjectTask.class);

    private DeploymentSpec ds;

    public ValidateDSProjectTask create(DeploymentSpec ds) {
        ValidateDSProjectTask task = new ValidateDSProjectTask();
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
            this.log.info("Validating the DS project " + this.ds.getProjectName() + " exists.");

            try (Openstack4jKeystone keystone = new Openstack4jKeystone(new Endpoint(vc))) {
                Project project = keystone.getProjectById(this.ds.getProjectId());
                if (project == null) {
                    this.log.info("DS project " + this.ds.getProjectName() + " Deleted from openstack. Marking DS for deletion.");
                    // project was deleted, mark ds for deleting as well
                    OSCEntityManager.markDeleted(em, this.ds, this.txBroadcastUtil);
                } else {
                    // Sync the project name if needed
                    if (!project.getName().equals(this.ds.getProjectName())) {
                        this.log.info("DS project name updated from " + this.ds.getProjectName() + " to "
                                + project.getName());
                        this.ds.setProjectName(project.getName());
                        OSCEntityManager.update(em, this.ds, this.txBroadcastUtil);
                    }
                }
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Validating Deployment Specification '%s' for project '%s'", this.ds.getName(),
                this.ds.getProjectName());
    };

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }

}
