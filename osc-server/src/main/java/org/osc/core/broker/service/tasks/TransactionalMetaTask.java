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
package org.osc.core.broker.service.tasks;

import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.MetaTask;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osgi.service.transaction.control.ScopedWorkException;
import org.osgi.service.transaction.control.TransactionControl;

public abstract class TransactionalMetaTask implements MetaTask {

    protected String name;

    public TransactionalMetaTask() {
    }

    @Override
    public void execute() throws Exception {
        EntityManager em = HibernateUtil.getTransactionalEntityManager();
        TransactionControl txControl = HibernateUtil.getTransactionControl();
        try {
            txControl.required(() -> {
                    executeTransaction(em);
                    return null;
                });
        } catch (ScopedWorkException e) {
            // Unwrap the ScopedWorkException to get the cause from
            // the scoped work (i.e. the executeTransaction() call.
            throw e.as(Exception.class);
        }
    }

    public abstract void executeTransaction(EntityManager em) throws Exception;

    @Override
    public Set<LockObjectReference> getObjects() {
        return null;
    }

    @Override
    public String toString() {
        return "[" + this.name + "]";
    }

}
