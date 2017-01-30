package org.osc.core.broker.service.tasks.conformance.manager;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class CreateDomainTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(CreateDomainTask.class);

    private ApplianceManagerConnector mc;
    private Domain domain;

    public CreateDomainTask(ApplianceManagerConnector mc, Domain domain) {
        this.mc = mc;
        this.domain = domain;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        log.debug("Start excecuting CreateDomainTask Task. Domain '" + domain.getName() + "'");
        mc = (ApplianceManagerConnector) session.get(ApplianceManagerConnector.class, mc.getId());
        Domain newDomain = new Domain(mc);
        newDomain.setMgrId(domain.getMgrId());
        newDomain.setName(domain.getName());

        EntityManager.create(session, newDomain);
    }

    @Override
    public String getName() {
        return "Create Domain '" + domain.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(mc);
    }

}
