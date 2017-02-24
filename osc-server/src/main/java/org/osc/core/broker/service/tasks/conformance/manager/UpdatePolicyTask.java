package org.osc.core.broker.service.tasks.conformance.manager;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class UpdatePolicyTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(UpdatePolicyTask.class);

    private Policy policy;
    private String newName;

    public UpdatePolicyTask(Policy policy, String newName) {
        this.policy = policy;
        this.newName = newName;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        log.debug("Start excecuting UpdatePolicyTask Task. Policy '" + policy.getName() + "'");
        policy = (Policy) session.get(Policy.class, policy.getId());
        policy.setName(newName);
        EntityManager.update(session, policy);
    }

    @Override
    public String getName() {
        return "Update Policy '" + policy.getName() + "' in Domain '" + policy.getDomain().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(policy.getApplianceManagerConnector());
    }

}
