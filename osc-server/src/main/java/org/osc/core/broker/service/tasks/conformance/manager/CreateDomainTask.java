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

import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

@Component(service=CreateDomainTask.class)
public class CreateDomainTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(CreateDomainTask.class);

    private ApplianceManagerConnector mc;
    private Domain domain;

    public CreateDomainTask create(ApplianceManagerConnector mc, Domain domain) {
        CreateDomainTask task = new CreateDomainTask();

        task.mc = mc;
        task.domain = domain;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        log.debug("Start excecuting CreateDomainTask Task. Domain '" + this.domain.getName() + "'");
        this.mc = em.find(ApplianceManagerConnector.class, this.mc.getId());
        Domain newDomain = new Domain(this.mc);
        newDomain.setMgrId(this.domain.getMgrId());
        newDomain.setName(this.domain.getName());

        OSCEntityManager.create(em, newDomain, this.txBroadcastUtil);
    }

    @Override
    public String getName() {
        return "Create Domain '" + this.domain.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.mc);
    }

}
