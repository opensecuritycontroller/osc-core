package org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

class DeleteDSFromDbTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(DeleteDSFromDbTask.class);

    private DeploymentSpec ds;

    public DeleteDSFromDbTask(DeploymentSpec ds) {
        this.ds = ds;
        this.name = getName();
    }

    @Override
    public String getName() {
        return String.format("Delete Deployment Specification '%s'", this.ds.getName());
    }

    @Override
    public void executeTransaction(Session session) {
        log.debug("Start Executing DeleteDSFromDb Task : " + this.ds.getId());
        this.ds = (DeploymentSpec) session.get(DeploymentSpec.class, this.ds.getId());
        EntityManager.delete(session, this.ds);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }

}
