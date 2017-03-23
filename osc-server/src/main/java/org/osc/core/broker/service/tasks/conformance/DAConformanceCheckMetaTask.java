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
package org.osc.core.broker.service.tasks.conformance;

import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VirtualizationType;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.virtualsystem.ValidateNsxTask;

public class DAConformanceCheckMetaTask extends TransactionalMetaTask {

    private DistributedAppliance da;
    private TaskGraph tg;
    private final ApiFactoryService apiFactoryService;

    /**
     * Kicks off DA conformance. Assumes the appropriate locks have been acquired already.
     * @param da
     */
    public DAConformanceCheckMetaTask(DistributedAppliance da, ApiFactoryService apiFactoryService) {
        this.da = da;
        this.apiFactoryService = apiFactoryService;
        this.name = getName();
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        this.da = em.find(DistributedAppliance.class, this.da.getId());

        this.tg = new TaskGraph();
        for (VirtualSystem vs : this.da.getVirtualSystems()) {
            TaskGraph vsTaskGraph = new TaskGraph();
            if (vs.getVirtualizationConnector().getVirtualizationType() == VirtualizationType.VMWARE) {
                vsTaskGraph.addTask(new ValidateNsxTask(vs, this.apiFactoryService));
            }
            if (vs.getMarkedForDeletion()) {
                vsTaskGraph.appendTask(new VSConformanceCheckMetaTask(vs, this.apiFactoryService), TaskGuard.ALL_PREDECESSORS_COMPLETED);
            } else {
                vsTaskGraph.appendTask(new VSConformanceCheckMetaTask(vs, this.apiFactoryService));
            }
            this.tg.addTaskGraph(vsTaskGraph);
        }

    }


    @Override
    public String getName() {
        return "Checking Virtual Systems for Distributed Appliance '" + this.da.getName() + "'";
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.da);
    }

}
