/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.service.tasks.conformance.manager;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.InfoTask;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.sdk.manager.api.ManagerDomainApi;
import org.osc.sdk.manager.element.ManagerDomainElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service=SyncDomainMetaTask.class)
public class SyncDomainMetaTask extends TransactionalMetaTask {

    //private static final Logger log = LoggerFactory.getLogger(SyncDomainMetaTask.class);

    @Reference
    CreateDomainTask createDomainTask;

    @Reference
    DeleteDomainTask deleteDomainTask;

    @Reference
    private ApiFactoryService apiFactoryService;

    private ApplianceManagerConnector mc;
    private TaskGraph tg;

    public SyncDomainMetaTask create(ApplianceManagerConnector mc) {
        SyncDomainMetaTask task = new SyncDomainMetaTask();
        task.mc = mc;
        task.name = task.getName();
        task.createDomainTask = this.createDomainTask;
        task.deleteDomainTask = this.deleteDomainTask;
        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        this.tg = new TaskGraph();

        this.mc = em.find(ApplianceManagerConnector.class, this.mc.getId());
        ManagerDomainApi mgrApi = this.apiFactoryService.createManagerDomainApi(this.mc);

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
                this.tg.appendTask(this.createDomainTask.create(this.mc, domain));
            } else {
                // Update policy attributes
                if (!domain.getName().equals(mgrDomain.getName())) {
                    domain.setName(mgrDomain.getName());
                    OSCEntityManager.update(em, domain, this.txBroadcastUtil);
                    this.tg.appendTask(new InfoTask("Updated Domain name ('" + mgrDomain.getName() + "')"));
                }
            }
        }


        for (Domain domain : domains) {
            ManagerDomainElement mgrDomain = findByMgrDomainId(mgrDomains, domain.getMgrId());
            if (mgrDomain == null) {
                // Delete domain
                this.tg.appendTask(this.deleteDomainTask.create(domain));
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
        return LockObjectReference.getObjectReferences(this.mc);
    }

}
