package org.osc.core.broker.service.tasks.conformance.manager;

import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.InfoTask;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.sdk.manager.api.ManagerDomainApi;
import org.osc.sdk.manager.element.ManagerDomainElement;

public class SyncDomainMetaTask extends TransactionalMetaTask {

    //private static final Logger log = Logger.getLogger(SyncDomainMetaTask.class);

    private ApplianceManagerConnector mc;
    private TaskGraph tg;

    public SyncDomainMetaTask(ApplianceManagerConnector mc) {
        this.mc = mc;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        this.tg = new TaskGraph();

        this.mc = (ApplianceManagerConnector) session.get(ApplianceManagerConnector.class, mc.getId());
        ManagerDomainApi mgrApi = ManagerApiFactory.createManagerDomainApi(this.mc);

        Set<Domain> domains = this.mc.getDomains();
        List<? extends ManagerDomainElement> mgrDomains = mgrApi.listDomains();
        if (mgrDomains == null) {
            return;
        }

        for (ManagerDomainElement mgrDomain : mgrDomains) {
            Domain domain = findByMgrDomainId(domains, mgrDomain.getId());
            if (domain == null) {
                // Add new domain
                domain = new Domain(this.mc);
                domain.setName(mgrDomain.getName());
                domain.setMgrId(mgrDomain.getId().toString());
                this.tg.appendTask(new CreateDomainTask(this.mc, domain));
            } else {
                // Update policy attributes
                if (!domain.getName().equals(mgrDomain.getName())) {
                    domain.setName(mgrDomain.getName());
                    EntityManager.update(session, domain);
                    this.tg.appendTask(new InfoTask("Updated Domain name ('" + mgrDomain.getName() + "')"));
                }
            }
        }


        for (Domain domain : domains) {
            ManagerDomainElement mgrDomain = findByMgrDomainId(mgrDomains, domain.getMgrId());
            if (mgrDomain == null) {
                // Delete domain
                this.tg.appendTask(new DeleteDomainTask(domain));
            }
        }

    }

    private ManagerDomainElement findByMgrDomainId(List<? extends ManagerDomainElement> mgrDomains, String mgrDomainId) {
        for (ManagerDomainElement mgrDomain : mgrDomains) {
            if (mgrDomain.getId().toString().equals(mgrDomainId)) {
                return mgrDomain;
            }
        }
        return null;
    }

    private Domain findByMgrDomainId(Set<Domain> domains, String mgrDomainId) {
        for (Domain domain : domains) {
            if (domain.getMgrId().equals(mgrDomainId)) {
                return domain;
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "Syncing Domains for Manager Connector '" + this.mc.getName() + "'";
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(mc);
    }

}
