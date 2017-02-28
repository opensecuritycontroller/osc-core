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

import java.util.List;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.virtualization.openstack.Network;
import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;
import org.osc.core.broker.service.persistence.NetworkEntityManager;
import org.osc.core.broker.service.persistence.SubnetEntityManager;
import org.osc.core.broker.service.persistence.VMPortEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

class MarkStalePortsAsDeletedTask extends TransactionalTask {

    private Network network;
    private Subnet subnet;
    private List<String> validPorts;

    public MarkStalePortsAsDeletedTask(Network network, List<String> validPorts) {
        this.network = network;
        this.validPorts = validPorts;
    }

    public MarkStalePortsAsDeletedTask(Subnet subnet, List<String> validPorts) {
        this.subnet = subnet;
        this.validPorts = validPorts;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        if (this.subnet == null) {

            this.network = NetworkEntityManager.findById(session, this.network.getId());
            //TODO: Future. Openstack. If we encounter stale object exception because we have multiple tasks trying to update
            // the same ports, we might need to retry with new transaction.
            VMPortEntityManager.markStalePortsAsDeleted(session, this.network, this.validPorts);
        }
        if (this.network == null) {
            this.subnet = SubnetEntityManager.findById(session, this.subnet.getId());
            //TODO: Future. Openstack. If we encounter stale object exception because we have multiple tasks trying to update
            // the same ports, we might need to retry with new transaction.
            //TODO mark stale VM for deletion ?
            VMPortEntityManager.markStalePortsAsDeletedForSubnet(session, this.subnet, this.validPorts);
        }

    }

    @Override
    public String getName() {
        if (this.network == null) {
            return String.format("Checking and Marking stale ports belonging to SUBNET '%s' as deleted",
                    this.subnet.getName());
        } else {
            return String.format("Checking and Marking stale ports belonging to NETWORK '%s' as deleted",
                    this.network.getName());
        }

    }

}
