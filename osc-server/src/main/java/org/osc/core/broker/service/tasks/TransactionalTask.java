package org.osc.core.broker.service.tasks;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.HibernateUtil;

public abstract class TransactionalTask extends BaseTask {

    public TransactionalTask() {
        super(null);
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

    @Override
    public String toString() {
        return "[" + name + "]";
    }

    public abstract void executeTransaction(Session session) throws Exception;

}
