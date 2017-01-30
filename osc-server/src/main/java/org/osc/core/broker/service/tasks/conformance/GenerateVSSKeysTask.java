package org.osc.core.broker.service.tasks.conformance;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.util.PKIUtil;

public class GenerateVSSKeysTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(GenerateVSSKeysTask.class);

    private VirtualSystem vs;

    public GenerateVSSKeysTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        log.debug("Start executing GetServiceInstanceTask");
        vs = (VirtualSystem) session.get(VirtualSystem.class, vs.getId());

        // generate and persist keys
        vs.setKeyStore(PKIUtil.generateKeyStore());
        EntityManager.update(session, vs);
    }

    @Override
    public String getName() {
        return "Register Service Instance '" + vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(vs);
    }

}
