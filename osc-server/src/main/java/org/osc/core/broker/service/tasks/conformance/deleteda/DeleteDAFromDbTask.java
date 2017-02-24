package org.osc.core.broker.service.tasks.conformance.deleteda;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class DeleteDAFromDbTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(DeleteDAFromDbTask.class);

    private DistributedAppliance da;

    public DeleteDAFromDbTask(DistributedAppliance da) {
        this.da = da;
        this.name = getName();
    }

    @Override
    public String getName() {
        return "Delete Distributed Appliance '" + da.getName() + "'";
    }

    @Override
    public void executeTransaction(Session session) {
        log.debug("Start Executing DeleteDAFromDb Task for DA: " + da.getId());
        da = (DistributedAppliance) session.get(DistributedAppliance.class, da.getId());
        EntityManager.delete(session, da);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.da);
    }

}
