package org.osc.core.broker.service.tasks.conformance.securitygroupinterface;

import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;

public class SecurityGroupCleanupCheckMetaTask extends TransactionalMetaTask {

    private VirtualSystem vs;
    private TaskGraph tg;

    public SecurityGroupCleanupCheckMetaTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        this.tg = new TaskGraph();

        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());

        for (SecurityGroupInterface sgi : this.vs.getSecurityGroupInterfaces()) {
            this.tg.appendTask(new DeleteSecurityGroupInterfaceTask(sgi));
        }
    }

    @Override
    public String getName() {
        return "Cleaning Traffic Policy Mappings on Virtual System '" + this.vs.getVirtualizationConnector().getName()
                + "'";
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
