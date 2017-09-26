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
package org.osc.core.broker.service.tasks.conformance.k8s.securitygroup;

import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.k8s.Label;
import org.osc.core.broker.model.entities.virtualization.k8s.Pod;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

/**
 * This task is responsible for deleting a pod and pod port from the OSC DB and it is
 * triggered when a pod is removed from Kubernetes matching the SGM label. If the
 * pod entity is associated with another security group member of the same security group
 * the pod will not be deleted.
 */
@Component(service = DeleteK8sLabelPodTask.class)
public class DeleteK8sLabelPodTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(CreateK8sLabelPodTask.class);

    private Pod pod;
    Label label;

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        OSCEntityManager<Pod> dsEmgr = new OSCEntityManager<Pod>(Pod.class, em, this.txBroadcastUtil);
        this.pod = dsEmgr.findByPrimaryKey(this.pod.getId());

        // If the pod contains any label that is not the same as this label, do not delete the pod. NOOP
        if (this.pod.getLabels().stream().anyMatch(label -> !label.getValue().equals(this.label.getValue()))) {
            LOG.info(String.format("The pod %s is associated with another security group member. Skipping deletion.", this.pod.getName()));
            return;
        }

        OSCEntityManager.delete(em, this.pod.getPorts().iterator().next(), this.txBroadcastUtil);
        OSCEntityManager.delete(em, this.pod, this.txBroadcastUtil);
    }

    public DeleteK8sLabelPodTask create(Pod pod, Label label) {
        DeleteK8sLabelPodTask task = new DeleteK8sLabelPodTask();
        task.pod = pod;
        task.label = label;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;
        return task;
    }

    @Override
    public String getName() {
        return String.format("Deleting the pod %s", this.pod.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.label);
    }
}
