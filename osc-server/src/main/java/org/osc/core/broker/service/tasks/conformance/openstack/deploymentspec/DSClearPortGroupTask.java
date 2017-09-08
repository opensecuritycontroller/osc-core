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

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

/**
 * If SFC chaining is enabled, we need to make sure that after the last inspection port
 * is deleted, the PortGroup id is cleared from the DeploymentSpec.
 *
 * This task should be scheduled after all the deletions of inspection ports have completed.
 */
@Component(service = DSClearPortGroupTask.class)
public class DSClearPortGroupTask extends TransactionalTask {

    private DeploymentSpec ds;

    public DSClearPortGroupTask create(DeploymentSpec ds) {
        DSClearPortGroupTask task = new DSClearPortGroupTask();
        task.ds = ds;
        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        if (this.ds.getDistributedApplianceInstances().size() == 0) {
            this.ds.setRedirectionTargetId(null);
            em.merge(this.ds);
        }
    }
}
