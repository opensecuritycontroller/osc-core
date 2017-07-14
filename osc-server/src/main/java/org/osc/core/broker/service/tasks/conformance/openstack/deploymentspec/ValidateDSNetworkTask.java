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
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNeutron;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

import javax.persistence.EntityManager;
import java.util.Set;

/**
 * Validates the DS Network exists and syncs the name if needed
 */
@Component(service = ValidateDSNetworkTask.class)
public class ValidateDSNetworkTask extends TransactionalTask {

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
    private Endpoint endPoint;
    private NetworkType networkType;

    public ValidateDSNetworkTask create(DeploymentSpec ds, Endpoint endPoint, NetworkType networkType) {
        ValidateDSNetworkTask task = new ValidateDSNetworkTask();
        task.ds = ds;
        task.endPoint = endPoint;
        task.networkType = networkType;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        OSCEntityManager<DeploymentSpec> dsEmgr = new OSCEntityManager<DeploymentSpec>(DeploymentSpec.class, em, this.txBroadcastUtil);
        this.ds = dsEmgr.findByPrimaryKey(this.ds.getId());

        if (!this.ds.getMarkedForDeletion()) {

            String networkName = this.networkType == NetworkType.MANAGEMENT ? this.ds.getManagementNetworkName()
                    : this.ds.getInspectionNetworkName();
            String networkId = this.networkType == NetworkType.MANAGEMENT ? this.ds.getManagementNetworkId() : this.ds
                    .getInspectionNetworkId();

            this.log.info("Validating the DS " + this.networkType + " network " + networkName + " exists.");

            try (Openstack4JNeutron neutron = new Openstack4JNeutron(this.endPoint)) {
                org.openstack4j.model.network.Network neutronNetwork = neutron.getNetworkById(this.ds.getRegion(), networkId);
                if (neutronNetwork == null) {
                    this.log.info("DS " + this.networkType + " network " + networkName
                            + " Deleted from openstack. Marking DS for deletion.");
                    // network was deleted, mark ds for deleting as well
                    OSCEntityManager.markDeleted(em, this.ds, this.txBroadcastUtil);
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
                        OSCEntityManager.update(em, this.ds, this.txBroadcastUtil);
                    }
                }
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Validating Deployment Specification '%s' for %s network '%s'", this.ds.getName(),
                this.networkType.toString(), this.networkType == NetworkType.MANAGEMENT ? this.ds.getManagementNetworkName()
                        : this.ds.getInspectionNetworkName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }

}
