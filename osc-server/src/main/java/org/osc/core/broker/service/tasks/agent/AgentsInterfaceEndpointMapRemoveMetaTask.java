package org.osc.core.broker.service.tasks.agent;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;

public class AgentsInterfaceEndpointMapRemoveMetaTask extends TransactionalMetaTask {

    final Logger log = Logger.getLogger(AgentsInterfaceEndpointMapRemoveMetaTask.class);

    private SecurityGroupInterface sgi;
    private TaskGraph tg;

    public AgentsInterfaceEndpointMapRemoveMetaTask(SecurityGroupInterface sgi) {
        this.sgi = sgi;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.tg = new TaskGraph();

        this.sgi = (SecurityGroupInterface) session.get(SecurityGroupInterface.class, this.sgi.getId());

        if (this.sgi.getVirtualSystem().getMgrId() == null) {
            this.log.warn("Manager VSS Device is not yet present. Do nothing.");
            return;
        }
        // Trigger service profile policy assignment removal for all DAIs
        for (DistributedApplianceInstance dai : this.sgi.getVirtualSystem().getDistributedApplianceInstances()) {
            this.tg.addTask(new AgentInterfaceEndpointMapRemoveTask(dai, this.sgi.getTag()));
        }
    }

    @Override
    public String getName() {
        return "Removing Traffic Policy Mapping '" + this.sgi.getTag() + "' assignment for VS '"
                + this.sgi.getVirtualSystem().getVirtualizationConnector().getName() + "'";
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.sgi);
    }

}
