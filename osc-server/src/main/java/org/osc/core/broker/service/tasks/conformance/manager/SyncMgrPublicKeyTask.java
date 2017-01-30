package org.osc.core.broker.service.tasks.conformance.manager;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class SyncMgrPublicKeyTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(SyncMgrPublicKeyTask.class);

    private ApplianceManagerConnector mc;
    private byte[] bytes;

    public SyncMgrPublicKeyTask(ApplianceManagerConnector mc, byte[] bytes) {
        this.mc = mc;
        this.bytes = bytes;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        log.debug("Start excecuting SyncMgrPublicKeyTask Task. MC: '" + this.mc.getName() + "'");

        this.mc = (ApplianceManagerConnector) session.get(ApplianceManagerConnector.class, this.mc.getId(),
                new LockOptions(LockMode.PESSIMISTIC_WRITE));

        this.mc.setPublicKey(bytes);
        EntityManager.update(session, this.mc);
    }

    @Override
    public String getName() {
        return "Syncing public key Manager Connector '" + this.mc.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(mc);
    }

}
