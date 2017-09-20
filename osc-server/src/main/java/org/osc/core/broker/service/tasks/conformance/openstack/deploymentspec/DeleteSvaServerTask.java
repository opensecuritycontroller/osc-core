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
import org.openstack4j.model.compute.Server;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNeutron;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNova;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.runner.RabbitMQRunner;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.EntityManager;
import java.util.Set;

@Component(service = DeleteSvaServerTask.class)
public class DeleteSvaServerTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(DeleteSvaServerTask.class);

    // target ensures this only binds to active runner published by Server
    @Reference(target = "(active=true)")
    private RabbitMQRunner activeRunner;

    private DistributedApplianceInstance dai;
    private String region;

    /**
     * Deletes the SVA associated with the DAI from openstack
     *
     * @param region the region the sva belongs to
     * @param dai    distributed appliance instance
     */
    public DeleteSvaServerTask create(String region, DistributedApplianceInstance dai) {
        DeleteSvaServerTask task = new DeleteSvaServerTask();
        task.region = region;
        task.dai = dai;
        task.activeRunner = this.activeRunner;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.dai = DistributedApplianceInstanceEntityMgr.findById(em, this.dai.getId());

        Endpoint osEndPoint = new Endpoint(this.dai.getDeploymentSpec());
        try (Openstack4JNova nova = new Openstack4JNova(osEndPoint);
             Openstack4JNeutron neutron = new Openstack4JNeutron(osEndPoint)) {
            Server sva = null;
            String serverId = this.dai.getExternalId();

            if (serverId != null) {
                sva = nova.getServer(this.region, this.dai.getExternalId());
            }

            if (sva == null) {
                sva = nova.getServerByName(this.region, this.dai.getName());
            }

            if (sva != null) {
                serverId = sva.getId();
            }

            this.activeRunner.getOsDeploymentSpecNotificationRunner()
                    .removeIdFromListener(this.dai.getDeploymentSpec().getId(), serverId);

            this.log.info("Deleting Server " + serverId + " from region " + this.region);
            boolean serverDeleted = serverId == null || nova.terminateInstance(this.region, serverId);
            boolean ingressPortDeleted = true;
            boolean egressPortDeleted = true;

            String ingressPortId = this.dai.getInspectionOsIngressPortId();
            String egressPortId = this.dai.getInspectionOsEgressPortId();

            if (ingressPortId != null) {
                this.log.info("Deleting Ingress port " + ingressPortId + " from region " + this.region);
                ingressPortDeleted = neutron.deletePortById(this.region, ingressPortId);
            }
            if (!this.dai.isSingleNicInspection() && egressPortId != null) {
                this.log.info("Deleting Egress port " + egressPortId + " from region " + this.region);
                egressPortDeleted = neutron.deletePortById(this.region, egressPortId);
            }

            if (!serverDeleted) {
                // Check if server still exists, if it does, fail!
                if (nova.getServer(this.region, serverId) != null) {
                    throw new VmidcException("Server: " + serverId + " deletion failed!");
                }
                this.log.warn("Server: " + serverId + " deletion failed from region: " + this.region
                        + ". Assume already deleted.");
            }

            if (!ingressPortDeleted || !egressPortDeleted) {
                this.log.info("Deleting ports failed. Ingress port deleted: " + ingressPortDeleted + " Egress port deleted:"
                        + egressPortDeleted);
                throw new VmidcException("Server: " + serverId + " port deletion failed!");
            }
        }


        this.dai.resetAllDiscoveredAttributes();
    }

    @Override
    public String getName() {
        return String.format("Deleting Server '%s' from region '%s'", this.dai.getName(), this.region);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
