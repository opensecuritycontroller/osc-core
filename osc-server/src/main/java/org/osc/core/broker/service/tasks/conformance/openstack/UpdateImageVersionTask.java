package org.osc.core.broker.service.tasks.conformance.openstack;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class UpdateImageVersionTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(UpdateImageVersionTask.class);

    private OsImageReference imageReference;
    private VirtualSystem vs;

    public UpdateImageVersionTask(OsImageReference imageReference, VirtualSystem vs) {
        this.imageReference = imageReference;
        this.vs = vs;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        log.debug("Start executing UpdateImageVersionTask with image reference ID " + this.imageReference.getId());

        this.imageReference = (OsImageReference) session.get(OsImageReference.class, this.imageReference.getId(),
                new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());
        this.imageReference.setApplianceVersion(this.vs.getApplianceSoftwareVersion());

        EntityManager.update(session, this.imageReference);
    }

    @Override
    public String getName() {
        return "Update OS Image Reference version with " + this.imageReference.getApplianceVersion();
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.imageReference.getVirtualSystem());
    }
}