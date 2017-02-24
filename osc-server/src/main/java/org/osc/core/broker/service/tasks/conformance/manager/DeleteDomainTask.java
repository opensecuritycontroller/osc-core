package org.osc.core.broker.service.tasks.conformance.manager;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class DeleteDomainTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(DeleteDomainTask.class);

    private Domain domain;

    public DeleteDomainTask(Domain domain) {
        this.domain = domain;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        log.debug("Start excecuting DeleteDomainTask Task. Domain '" + domain.getName() + "'");
        domain = (Domain) session.get(Domain.class, domain.getId());
        EntityManager.delete(session, domain);
    }

    @Override
    public String getName() {
        return "Delete Domain '" + domain.getName() + "'";
    }

}
