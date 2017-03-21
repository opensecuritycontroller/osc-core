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
package org.osc.core.broker.service.tasks.conformance.openstack;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.jclouds.openstack.neutron.v2.domain.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.OsSecurityGroupReference;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNeutron;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class DeleteOsSecurityGroupTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(DeleteOsSecurityGroupTask.class);

    private static final int SLEEP_RETRIES = 5 * 1000; // 5 seconds
    private static final int MAX_ATTEMPTS = 3;

    private DeploymentSpec ds;
    private OsSecurityGroupReference sgReference;

    public DeleteOsSecurityGroupTask(DeploymentSpec ds, OsSecurityGroupReference sgReference) {
        this.ds = ds;
        this.sgReference = sgReference;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        int count = MAX_ATTEMPTS;

        boolean osSgCanBeDeleted = DeploymentSpecEntityMgr.findDeploymentSpecsByVirtualSystemTenantAndRegion(em,
                this.ds.getVirtualSystem(), this.ds.getTenantId(), this.ds.getRegion()).size() <= 1;

        if (osSgCanBeDeleted) {
            this.log.info(String.format("Deleting Openstack Security Group with id '%s' from region '%s'",
                    this.sgReference.getSgRefId(), this.ds.getRegion()));

            this.sgReference = em.find(OsSecurityGroupReference.class,
                    this.sgReference.getId());

            Endpoint endPoint = new Endpoint(this.ds);
            try (JCloudNeutron neutron = new JCloudNeutron(endPoint)) {
                boolean success = false;
                // check if the security group exist on Openstack
                SecurityGroup osSg = neutron.getSecurityGroupById(this.ds.getRegion(), this.sgReference.getSgRefId());
                if (osSg != null) {
                    while (!success) {
                        try {
                            success = neutron.deleteSecurityGroupById(this.ds.getRegion(),
                                    this.sgReference.getSgRefId());
                        } catch (IllegalStateException ex) {
                            this.log.info(" Openstack Security Group id:" + this.sgReference.getSgRefId() + " in use.");
                            Thread.sleep(SLEEP_RETRIES);
                        } finally {
                            if (--count <= 0) {
                                throw (new Exception("Unable to delete the Openstack Security Group id: "
                                        + this.sgReference.getSgRefId()));
                            }
                        }
                    }
                }
            }

            for (DeploymentSpec ds : this.sgReference.getDeploymentSpecs()) {
                ds.setOsSecurityGroupReference(null);
                OSCEntityManager.update(em, ds);
            }
            this.sgReference.getDeploymentSpecs().clear();
            OSCEntityManager.delete(em, this.sgReference);
        }
    }

    @Override
    public String getName() {
        return String.format("Deleting Openstack Security Group with id '%s' from tenant '%s' in region '%s'",
                this.sgReference.getSgRefId(), this.ds.getTenantName(), this.ds.getRegion());

    };

}
