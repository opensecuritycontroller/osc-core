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
package org.osc.core.broker.service.transactions;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.model.entities.job.LastJobContainer;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.util.TransactionalBroadcastUtil;

/**
 * Attaches (in transaction) the last job record to given entity
 * @param <T> type that implements LastJobContainer (IscEntity with ability to attach last job)
 */
public class CompleteJobTransaction<T extends LastJobContainer> {
    private Class<T> entityType;

    private TransactionalBroadcastUtil txBroadcastUtil;

    public CompleteJobTransaction(Class<T> entityType, TransactionalBroadcastUtil txBroadcastUtil) {
        this.entityType = entityType;
        this.txBroadcastUtil = txBroadcastUtil;
    }

    public Void run(EntityManager em, CompleteJobTransactionInput param) throws Exception {
        T entity = em.find(this.entityType, param.getEntityId());

        if (entity == null) {
            throw new IllegalArgumentException(String.format("Entity with Id '%d' does not exist.", param.getEntityId()));
        }

        JobRecord jobRecord = em.find(JobRecord.class, param.getJobId());

        if (jobRecord == null) {
            throw new IllegalArgumentException(String.format("Job record with id '%d' does not exist.", param.getJobId()));
        }

        entity.setLastJob(em.find(JobRecord.class, param.getJobId()));
        OSCEntityManager.update(em, entity, this.txBroadcastUtil);

        return null;
    }
}