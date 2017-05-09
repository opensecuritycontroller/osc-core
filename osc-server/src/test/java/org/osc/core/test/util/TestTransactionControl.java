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
package org.osc.core.test.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Assert;
import org.osgi.service.transaction.control.ScopedWorkException;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionException;
import org.osgi.service.transaction.control.TransactionRolledBackException;
import org.osgi.service.transaction.control.TransactionStatus;

public abstract class TestTransactionControl implements TransactionControl, TransactionContext {

    private EntityManager em;

    private AtomicBoolean txActive = new AtomicBoolean();

    private Map<Object, Object> context = new HashMap<>();

    private List<Consumer<TransactionStatus>> postCompletionCallbacks = new ArrayList<>();

    public void setEntityManager(EntityManager em) {
        this.em = em;
        this.txActive = new AtomicBoolean();
        this.context = new HashMap<>();
        this.postCompletionCallbacks = new ArrayList<>();
    }

    @Override
    public <T> T required(Callable<T> arg0)
            throws TransactionException, TransactionRolledBackException, ScopedWorkException {
        if(this.txActive.getAndSet(true)) {
            // inherit the existing tran
            try {
                return arg0.call();
            } catch (Exception e) {
                if(e instanceof ScopedWorkException) {
                    throw (ScopedWorkException) e;
                }
                throw new ScopedWorkException("The work failed", e, getCurrentContext());
            }
        } else {
            return runInTran(arg0);
        }
    }

    private <T> T runInTran(Callable<T> arg0) {
        EntityTransaction tx = this.em.getTransaction();
        try {
            tx.begin();
            T o = arg0.call();
            tx.commit();

            callListeners(TransactionStatus.COMMITTED);

            return o;
        } catch (Exception e) {
            tx.rollback();
            callListeners(TransactionStatus.ROLLED_BACK);
            if(e instanceof ScopedWorkException) {
                throw (ScopedWorkException) e;
            }
            throw new ScopedWorkException("The work failed", e, getCurrentContext());
        } finally {
            this.txActive.compareAndSet(true, false);
            this.context.clear();
            this.em.clear();
        }
    }

    @Override
    public <T> T requiresNew(Callable<T> arg0)
            throws TransactionException, TransactionRolledBackException, ScopedWorkException {
        if(this.txActive.getAndSet(true)) {
            Assert.fail("The test transaction control does not support nested transactions");

            // This line is never actually reached but is needed by the compiler
            return null;
        } else {
            return runInTran(arg0);
        }
    }

    private void callListeners(TransactionStatus committed) {
        for(Consumer<TransactionStatus> c : this.postCompletionCallbacks) {
            try {
                c.accept(committed);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.postCompletionCallbacks.clear();
    }

    @Override
    public boolean activeScope() {
        return this.txActive.get();
    }

    @Override
    public boolean activeTransaction() {
        return this.txActive.get();
    }

    @Override
    public TransactionContext getCurrentContext() {
        return activeScope() ? this : null;
    }

    @Override
    public Object getScopedValue(Object arg0) {
        return this.context.get(arg0);
    }

    @Override
    public void postCompletion(Consumer<TransactionStatus> arg0) throws IllegalStateException {
        this.postCompletionCallbacks.add(arg0);
    }

    @Override
    public void putScopedValue(Object arg0, Object arg1) {
        this.context.put(arg0, arg1);
    }
}
