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
package org.osc.core.broker.util.db;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

/**
 * TransactionalRunner ensures that the execution of given logic is secured by transaction and DB changes will be
 * rolled back in case of any exception.
 * @param <T> the type of output for transaction logic
 * @param <S> the type of input for transaction logic
 */
public class TransactionalRunner<T, S> {

    // INNER TYPES

    /** Interface that can provide and closeSession hibernate session */
    public interface SessionHandler {
        /** Obtains hibernate session */
        EntityManager getEntityManager();
        /** Recycles hibernate session **/
        void closeSession(EntityManager em);
    }

    /** Provides shared, opened hibernate session, doesn't close it
     *  Session is shared for all transactional runner instance executions */
    public static class SharedSessionHandler implements SessionHandler {

        private EntityManager em;

        @Override
        public EntityManager getEntityManager() {
            if(this.em == null) {
                this.em = HibernateUtil.getEntityManagerFactory().createEntityManager();
            }
            return this.em;
        }

        @Override
        public void closeSession(EntityManager em) {}
    }

    /** Creates session on demand before the transaction and closes after transaction.
     *  Session is created and closed per each TransactRunner execution */
    public static class ExclusiveSessionHandler implements SessionHandler {

        @Override
        public EntityManager getEntityManager() {
            return HibernateUtil.getEntityManagerFactory().createEntityManager();
        }

        @Override
        public void closeSession(EntityManager em) {
            if(em != null) {
                em.close();
            }
        }
    }

    /** TransactionalAction represents the action that runs under transaction
     * @param <T> the type of output for transaction logic
     * @param <S> the type of input for transaction logic
     */
    public interface TransactionalAction<T, S> {
        /**
         * Performs the action under transaction
         * @param session the session (session is either shared or exclusive depending on TransactionalRunner) session handling configuration
         * @param param the input for the action
         * @return the output for the action
         * @throws Exception
         */
        T run(EntityManager em, S param) throws Exception;
    }

    /**
     * TransactionalAction listener allows to inject handling execution events after successful transaction commit or
     * rollback
     */
    public interface TransactionalListener {
        /**
         * handles the logic just after successful commit
         *
         * @param session
         *            DB session
         */
        void afterCommit(EntityManager em);

        /**
         * handles the logic just after rollback
         *
         * @param session
         *            DB session
         */
        void afterRollback(EntityManager em);
    }

    /** Allows to inject custom error handing */
    public interface ErrorHandler {
        void handleError(Exception e);
    }

    // MEMBERS

    private Logger log = Logger.getLogger(TransactionalRunner.class);

    /** Empty listener */
    private TransactionalListener transactionalListener = new TransactionalListener() {
        @Override
        public void afterCommit(EntityManager em) { }

        @Override
        public void afterRollback(EntityManager em) { }
    };

    /** Empty error handler */
    private ErrorHandler errorHandler = new ErrorHandler() {
        @Override
        public void handleError(Exception e) {}
    };

    private SessionHandler sessionHandler;

    // CTORS

    /**
     * Ctor
     * @param sessionHandler defines if hibernate session should be created and closed per each TransactRunner execution
     * or should one use single, shared session for all transactional runner executions
     */
    public TransactionalRunner(SessionHandler sessionHandler) {
        this.sessionHandler = sessionHandler;
    }

    // METHODS

    /** Attaches transactional listener to handle transactional logic events */
    public TransactionalRunner<T, S> withTransactionalListener(TransactionalListener executionFilter) {
        this.transactionalListener = executionFilter;
        return this;
    }

    /** Sets the error handling strategy */
    public TransactionalRunner<T, S> withErrorHandling(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    /**
     * Executes parameterless transation logic
     * @return response object of transaction logic or null in case of exception from transaction logic
     */
    public T exec(TransactionalAction<T, S> logic) {
        return exec(logic, null);
    }

    /**
     * Executes transation logic with given input
     * @return response object of transaction logic or null in case of exception from transaction logic
     */
    public T exec(TransactionalAction<T, S> logic, S param) {

        EntityManager em = null;
        EntityTransaction transaction = null;
        T output = null;

        try {
            em = this.sessionHandler.getEntityManager();

            transaction = em.getTransaction();
            transaction.begin();

            // call some abstract action on input and produce output
            output = logic.run(em, param);

            transaction.commit();
            this.transactionalListener.afterCommit(em);
        } catch (Exception e) {

            this.log.error("Fail to execute transaction logic.", e);
            if (transaction != null) {
                transaction.rollback();
                this.transactionalListener.afterRollback(em);
            }

            this.errorHandler.handleError(e);

        } finally {
            this.sessionHandler.closeSession(em);
        }

        return output;
    }
}
