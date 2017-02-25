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

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.jclouds.openstack.neutron.v2.domain.Network;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNeutron;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

/**
 * Validates the DS tenant exists and syncs the name if needed
 */
class ValidateDSNetworkTask extends TransactionalTask {

    final Logger log = Logger.getLogger(ValidateDSNetworkTask.class);

    enum NetworkType {
        MANAGEMENT("Management"), INSPECTION("Inspection");

        private String name;

        NetworkType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }

    }

    private DeploymentSpec ds;
    private final Endpoint endPoint;
    private final NetworkType networkType;

    public ValidateDSNetworkTask(DeploymentSpec ds, Endpoint endPoint, NetworkType networkType) {
        this.ds = ds;
        this.endPoint = endPoint;
        this.networkType = networkType;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        EntityManager<DeploymentSpec> dsEmgr = new EntityManager<DeploymentSpec>(DeploymentSpec.class, session);
        this.ds = dsEmgr.findByPrimaryKey(this.ds.getId());

        if (!this.ds.getMarkedForDeletion()) {

            String networkName = this.networkType == NetworkType.MANAGEMENT ? this.ds.getManagementNetworkName()
                    : this.ds.getInspectionNetworkName();
            String networkId = this.networkType == NetworkType.MANAGEMENT ? this.ds.getManagementNetworkId() : this.ds
                    .getInspectionNetworkId();

            this.log.info("Validating the DS " + this.networkType + " network " + networkName + " exists.");

            JCloudNeutron neutron = new JCloudNeutron(this.endPoint);

            try {
                Network neutronNetwork = neutron.getNetworkById(this.ds.getRegion(), networkId);
                if (neutronNetwork == null) {
                    this.log.info("DS " + this.networkType + " network " + networkName
                            + " Deleted from openstack. Marking DS for deletion.");
                    // network was deleted, mark ds for deleting as well
                    EntityManager.markDeleted(session, this.ds);
                } else {
                    // Sync the network name if needed
                    if (!neutronNetwork.getName().equals(networkName)) {
                        this.log.info("DS " + this.networkType + " network name updated from " + networkName + " to "
                                + neutronNetwork.getName());
                        if (this.networkType == NetworkType.MANAGEMENT) {
                            this.ds.setManagementNetworkName(neutronNetwork.getName());
                        } else {
                            this.ds.setInspectionNetworkName(neutronNetwork.getName());
                        }
                        EntityManager.update(session, this.ds);
                    }
                }
            } finally {
                neutron.close();
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Validating Deployment Specification '%s' for %s network '%s'", this.ds.getName(),
                this.networkType.toString(), this.networkType == NetworkType.MANAGEMENT ? this.ds.getManagementNetworkName()
                        : this.ds.getInspectionNetworkName());
    };

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }

}
