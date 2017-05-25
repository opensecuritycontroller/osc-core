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

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

@Component(service=DeleteDomainTask.class)
public class DeleteDomainTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(DeleteDomainTask.class);

    private Domain domain;

    public DeleteDomainTask create(Domain domain) {
        DeleteDomainTask task = new DeleteDomainTask();
        task.domain = domain;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        log.debug("Start excecuting DeleteDomainTask Task. Domain '" + this.domain.getName() + "'");
        this.domain = em.find(Domain.class, this.domain.getId());
        OSCEntityManager.delete(em, this.domain, this.txBroadcastUtil);
    }

    @Override
    public String getName() {
        return "Delete Domain '" + this.domain.getName() + "'";
    }

}
