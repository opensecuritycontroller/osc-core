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

import org.apache.log4j.Logger;
import org.openstack4j.model.network.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.OsSecurityGroupReference;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNeutron;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

import javax.persistence.EntityManager;

@Component(service = DeleteOsSecurityGroupTask.class)
public class DeleteOsSecurityGroupTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(DeleteOsSecurityGroupTask.class);

    private static final int SLEEP_RETRIES = 5 * 1000; // 5 seconds
    private static final int MAX_ATTEMPTS = 3;

    private DeploymentSpec ds;
    private OsSecurityGroupReference sgReference;

    public DeleteOsSecurityGroupTask create(DeploymentSpec ds, OsSecurityGroupReference sgReference) {
        DeleteOsSecurityGroupTask task = new DeleteOsSecurityGroupTask();
        task.ds = ds;
        task.sgReference = sgReference;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        int count = MAX_ATTEMPTS;

        boolean osSgCanBeDeleted = DeploymentSpecEntityMgr.findDeploymentSpecsByVirtualSystemTenantAndRegion(em,
                this.ds.getVirtualSystem(), this.ds.getProjectId(), this.ds.getRegion()).size() <= 1;

        if (osSgCanBeDeleted) {
            this.log.info(String.format("Deleting Openstack Security Group with id '%s' from region '%s'",
                    this.sgReference.getSgRefId(), this.ds.getRegion()));

            this.sgReference = em.find(OsSecurityGroupReference.class,
                    this.sgReference.getId());

            Endpoint endPoint = new Endpoint(this.ds);
            try (Openstack4JNeutron neutron = new Openstack4JNeutron(endPoint)) {
                boolean success = false;
                // check if the security group exist on Openstack
                SecurityGroup osSg = neutron.getSecurityGroupById(this.ds.getRegion(), this.sgReference.getSgRefId());
                if (osSg != null) {
                    while (!success) {
                        try {
                            success = neutron.deleteSecurityGroupById(this.ds.getRegion(), this.sgReference.getSgRefId());
                        } catch (IllegalStateException ex) {
                            this.log.info("Failed to remove openstack Security Group: " + ex.getMessage());
                            Thread.sleep(SLEEP_RETRIES);
                        } finally {
                            if (--count <= 0) {
                                throw new Exception("Unable to delete the Openstack Security Group id: " + this.sgReference.getSgRefId());
                            }
                        }
                    }
                }
            }

            for (DeploymentSpec ds : this.sgReference.getDeploymentSpecs()) {
                ds.setOsSecurityGroupReference(null);
                OSCEntityManager.update(em, ds, this.txBroadcastUtil);
            }
            this.sgReference.getDeploymentSpecs().clear();
            OSCEntityManager.delete(em, this.sgReference, this.txBroadcastUtil);
        }
    }

    @Override
    public String getName() {
        return String.format("Deleting Openstack Security Group with id '%s' from tenant '%s' in region '%s'",
                this.sgReference.getSgRefId(), this.ds.getProjectName(), this.ds.getRegion());

    }

}
