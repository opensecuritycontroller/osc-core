package org.osc.core.broker.service.tasks.conformance.manager;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class CreatePolicyTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(CreatePolicyTask.class);

    private ApplianceManagerConnector mc;
    private Domain domain;
    private Policy policy;

    public CreatePolicyTask(ApplianceManagerConnector mc, Domain domain, Policy policy) {
        this.mc = mc;
        this.domain = domain;
        this.policy = policy;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        log.debug("Start excecuting CreatePolicyTask Task. Policy '" + policy.getName() + "'");
        mc = (ApplianceManagerConnector) session.get(ApplianceManagerConnector.class, mc.getId());

        Policy newPolicy = new Policy(mc, domain);
        newPolicy.setMgrPolicyId(policy.getMgrPolicyId());
        newPolicy.setName(policy.getName());
        domain.addPolicy(newPolicy);
        EntityManager.update(session, domain);
    }

    @Override
    public String getName() {
        return "Create Policy '" + policy.getName() + "' in Domain '" + domain.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(mc);
    }

}
