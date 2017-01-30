package org.osc.core.broker.service.tasks;

import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.osc.core.broker.job.MetaTask;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.HibernateUtil;

public abstract class TransactionalMetaTask implements MetaTask {

    protected String name;

    public TransactionalMetaTask() {
    }

    @Override
    public void execute() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();

        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            executeTransaction(session);
            tx.commit();
            TransactionalBroadcastUtil.broadcast(session);

        } catch (Exception ex) {

            if (tx != null) {
                tx.rollback();
                TransactionalBroadcastUtil.removeSessionFromMap(session);
            }

            throw ex;

        } finally {

            if (session != null) {
                session.close();
            }
        }
    }

    public abstract void executeTransaction(Session session) throws Exception;

    @Override
    public Set<LockObjectReference> getObjects() {
        return null;
    }

    @Override
    public String toString() {
        return "[" + name + "]";
    }

}
