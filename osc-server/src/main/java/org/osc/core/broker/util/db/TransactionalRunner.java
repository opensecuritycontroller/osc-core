package org.osc.core.broker.util.db;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

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
        Session getSession();
        /** Recycles hibernate session **/
        void closeSession(Session session);
    }

    /** Provides shared, opened hibernate session, doesn't close it
     *  Session is shared for all transactional runner instance executions */
    public static class SharedSessionHandler implements SessionHandler {

        @Override
        public Session getSession() {
            SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
            return sessionFactory.getCurrentSession();
        }

        @Override
        public void closeSession(Session session) {}
    }

    /** Creates session on demand before the transaction and closes after transaction.
     *  Session is created and closed per each TransactRunner execution */
    public static class ExclusiveSessionHandler implements SessionHandler {

        @Override
        public Session getSession() {
            return HibernateUtil.getSessionFactory().openSession();
        }

        @Override
        public void closeSession(Session session) {
            if(session != null) {
                session.close();
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
        T run(Session session, S param) throws Exception;
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
        void afterCommit(Session session);

        /**
         * handles the logic just after rollback
         *
         * @param session
         *            DB session
         */
        void afterRollback(Session session);
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
        public void afterCommit(Session session) { }

        @Override
        public void afterRollback(Session session) { }
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

        Session session = null;
        Transaction transaction = null;
        T output = null;

        try {
            session = this.sessionHandler.getSession();

            transaction = session.beginTransaction();

            // call some abstract action on input and produce output
            output = logic.run(session, param);

            transaction.commit();
            this.transactionalListener.afterCommit(session);
        } catch (Exception e) {

            this.log.error("Fail to execute transaction logic.", e);
            if (transaction != null) {
                transaction.rollback();
                this.transactionalListener.afterRollback(session);
            }

            this.errorHandler.handleError(e);

        } finally {
            this.sessionHandler.closeSession(session);
        }

        return output;
    }
}
