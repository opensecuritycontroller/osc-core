package org.osc.core.broker.service.tasks.conformance;

import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.virtualsystem.ValidateNsxTask;

public class DAConformanceCheckMetaTask extends TransactionalMetaTask {

    private DistributedAppliance da;
    private TaskGraph tg;

    /**
     * Kicks off DA conformance. Assumes the appropriate locks have been acquired already.
     * @param da
     */
    public DAConformanceCheckMetaTask(DistributedAppliance da) {
        this.da = da;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        this.da = (DistributedAppliance) session.get(DistributedAppliance.class, this.da.getId());

        this.tg = new TaskGraph();
        for (VirtualSystem vs : this.da.getVirtualSystems()) {
            TaskGraph vsTaskGraph = new TaskGraph();
            if (vs.getVirtualizationConnector().isVmware()) {
                vsTaskGraph.addTask(new ValidateNsxTask(vs));
            }
            if (vs.getMarkedForDeletion()) {
                vsTaskGraph.appendTask(new VSConformanceCheckMetaTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED);
            } else {
                vsTaskGraph.appendTask(new VSConformanceCheckMetaTask(vs));
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
