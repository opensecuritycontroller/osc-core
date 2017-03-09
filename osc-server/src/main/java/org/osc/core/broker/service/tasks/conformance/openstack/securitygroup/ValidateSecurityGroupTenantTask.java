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
import org.jclouds.openstack.keystone.v2_0.domain.Tenant;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudKeyStone;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

/**
 * Validates the DS tenant exists and syncs the name if needed
 */
class ValidateSecurityGroupTenantTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(ValidateSecurityGroupTenantTask.class);

    private SecurityGroup securityGroup;

    public ValidateSecurityGroupTenantTask(SecurityGroup securityGroup) {
        this.securityGroup = securityGroup;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        OSCEntityManager<SecurityGroup> sgEmgr = new OSCEntityManager<SecurityGroup>(SecurityGroup.class, em);
        this.securityGroup = sgEmgr.findByPrimaryKey(this.securityGroup.getId());

        this.log.info("Validating the Security Group tenant " + this.securityGroup.getTenantName() + " exists.");
        JCloudKeyStone keystone = new JCloudKeyStone(new Endpoint(this.securityGroup.getVirtualizationConnector()));

        try {
            Tenant tenant = keystone.getTenantById(this.securityGroup.getTenantId());
            if (tenant == null) {
                this.log.info("Security Group tenant " + this.securityGroup.getTenantName() + " Deleted from openstack. Marking Security Group for deletion.");
                // Tenant was deleted, mark Security Group for deleting as well
                OSCEntityManager.markDeleted(em, this.securityGroup);
            } else {
                // Sync the tenant name if needed
                if (!tenant.getName().equals(this.securityGroup.getTenantName())) {
                    this.log.info("Security Group tenant name updated from " + this.securityGroup.getTenantName() + " to " + tenant.getName());
                    this.securityGroup.setTenantName(tenant.getName());
                    OSCEntityManager.update(em, this.securityGroup);
                }
            }

        } finally {
            keystone.close();
        }
    }

    @Override
    public String getName() {
        return String.format("Validating Security Group '%s' for tenant '%s'", this.securityGroup.getName(), this.securityGroup.getTenantName());
    };


    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.securityGroup);
    }

}
