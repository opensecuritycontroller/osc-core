package org.osc.core.broker.service.tasks.conformance.deleteda;

import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class DeleteVsFromDbTask extends TransactionalTask {
    //private static final Logger log = Logger.getLogger(DeleteVsFromDbTask.class);

    private VirtualSystem vs;

    public DeleteVsFromDbTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());
        if (this.vs.getSecurityGroupInterfaces().size() > 0) {
            throw new VmidcBrokerInvalidRequestException(String.format(
                    "Virtual System '%s' has Traffic Policy Mappings which"
                            + " need to be unbinded before the Virtual system can be deleted", this.vs
                            .getVirtualizationConnector().getName()));
        }
        EntityManager.delete(session, this.vs);
    }

    @Override
    public String getName() {
        return "Delete Virtualization System '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
