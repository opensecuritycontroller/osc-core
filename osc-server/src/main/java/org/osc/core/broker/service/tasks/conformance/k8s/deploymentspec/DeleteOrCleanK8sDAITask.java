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
package org.osc.core.broker.service.tasks.conformance.k8s.deploymentspec;

import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.rest.client.k8s.KubernetesDeploymentApi;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

/**
 * This task is responsible for deleting or cleaning up a DAI in the OSC database for a deleted pod.
 */
@Component(service = DeleteOrCleanK8sDAITask.class)
public class DeleteOrCleanK8sDAITask extends TransactionalTask {
    private DistributedApplianceInstance dai;

    public DeleteOrCleanK8sDAITask create(DistributedApplianceInstance daiForDeletion) {
        return create(daiForDeletion, null);
    }

    DeleteOrCleanK8sDAITask create(DistributedApplianceInstance dai, KubernetesDeploymentApi k8sDeploymentApi) {
        DeleteOrCleanK8sDAITask task = new DeleteOrCleanK8sDAITask();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;
        task.dai = dai;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        OSCEntityManager<DistributedApplianceInstance> dsEmgr = new OSCEntityManager<DistributedApplianceInstance>(DistributedApplianceInstance.class, em, this.txBroadcastUtil);
        this.dai = dsEmgr.findByPrimaryKey(this.dai.getId());

        // If the DAI is associated with an existing inspection element on the SDN controller
        // do not delete it, instead clear out the network information.
        if (this.dai.getInspectionElementId() == null) {
            OSCEntityManager.delete(em, this.dai, this.txBroadcastUtil);
        } else {
            this.dai.setInspectionOsIngressPortId(null);
            this.dai.setInspectionIngressMacAddress(null);
            this.dai.setInspectionOsEgressPortId(null);
            this.dai.setInspectionEgressMacAddress(null);
            OSCEntityManager.update(em, this.dai, this.txBroadcastUtil);
        }
    }

    @Override
    public String getName() {
        return String.format("Deleting or cleaning the K8s dai %s", this.dai.getId());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }
}
