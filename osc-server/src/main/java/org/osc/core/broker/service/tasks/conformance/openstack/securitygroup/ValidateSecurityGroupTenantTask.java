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
package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.openstack4j.model.identity.v3.Project;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4jKeystone;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

/**
 * Validates the DS tenant exists and syncs the name if needed
 */
@Component(service = ValidateSecurityGroupTenantTask.class)
public class ValidateSecurityGroupTenantTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(ValidateSecurityGroupTenantTask.class);

    private SecurityGroup securityGroup;

    public ValidateSecurityGroupTenantTask create(SecurityGroup securityGroup) {
        ValidateSecurityGroupTenantTask task = new ValidateSecurityGroupTenantTask();
        task.securityGroup = securityGroup;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        OSCEntityManager<SecurityGroup> sgEmgr = new OSCEntityManager<SecurityGroup>(SecurityGroup.class, em, this.txBroadcastUtil);
        this.securityGroup = sgEmgr.findByPrimaryKey(this.securityGroup.getId());

        this.log.info("Validating the Security Group tenant " + this.securityGroup.getProjectName() + " exists.");
        try (Openstack4jKeystone keystone = new Openstack4jKeystone(new Endpoint(this.securityGroup.getVirtualizationConnector()))) {
            Project tenant = keystone.getProjectById(this.securityGroup.getProjectId());
            if (tenant == null) {
                this.log.info("Security Group tenant " + this.securityGroup.getProjectName() + " Deleted from openstack. Marking Security Group for deletion.");
                // Tenant was deleted, mark Security Group for deleting as well
                OSCEntityManager.markDeleted(em, this.securityGroup, this.txBroadcastUtil);
            } else {
                // Sync the tenant name if needed
                if (!tenant.getName().equals(this.securityGroup.getProjectName())) {
                    this.log.info("Security Group tenant name updated from " + this.securityGroup.getProjectName() + " to " + tenant.getName());
                    this.securityGroup.setProjectName(tenant.getName());
                    OSCEntityManager.update(em, this.securityGroup, this.txBroadcastUtil);
                }
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Validating Security Group '%s' for project '%s'", this.securityGroup.getName(), this.securityGroup.getProjectName());
    };


    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.securityGroup);
    }
}
