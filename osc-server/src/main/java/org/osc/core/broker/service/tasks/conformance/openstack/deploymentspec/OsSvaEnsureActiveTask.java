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

import javax.persistence.EntityManager;

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

/**
 * Ensures the SVA vm is active. Basically waits until the SVA is active for us to
 * follow up with other tasks which rely on the server being active and ready.
 * This is a transactional task but does not updates on the enities so should be safe from conflicts
 */
@Component(service=OsSvaEnsureActiveTask.class)
public class OsSvaEnsureActiveTask extends TransactionalTask {

    private DistributedApplianceInstance dai;

    public OsSvaEnsureActiveTask create(DistributedApplianceInstance dai) {
        OsSvaEnsureActiveTask task = new OsSvaEnsureActiveTask();
        task.dai = dai;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.dai = DistributedApplianceInstanceEntityMgr.findById(em, this.dai.getId());

        String osServerId = this.dai.getOsServerId();
        DeploymentSpec ds = this.dai.getDeploymentSpec();
        VirtualizationConnector vc = ds.getVirtualSystem().getVirtualizationConnector();

        String projectName = ds.getProjectName();
        String region = ds.getRegion();
        OpenstackUtil.ensureVmActive(vc, projectName, region, osServerId);
    }

    @Override
    public String getName() {
        return String.format("Ensuring SVA '%s' is active", this.dai.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
