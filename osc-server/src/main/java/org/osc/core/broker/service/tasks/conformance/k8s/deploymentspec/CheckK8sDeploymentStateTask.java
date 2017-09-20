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

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

@Component(service = CheckK8sDeploymentStateTask.class)
public class CheckK8sDeploymentStateTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(CheckK8sDeploymentStateTask.class);

    private DeploymentSpec ds;

    public CheckK8sDeploymentStateTask create(DeploymentSpec ds) {
        CheckK8sDeploymentStateTask task = new CheckK8sDeploymentStateTask();
        task.ds = ds;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
    }

    @Override
    public String getName() {
        return String.format("Checking the state of the K8s deployment spec '%s'", this.ds.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }
}
