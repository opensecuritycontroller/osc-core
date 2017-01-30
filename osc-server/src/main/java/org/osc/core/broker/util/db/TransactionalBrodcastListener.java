package org.osc.core.broker.util.db;

import org.hibernate.Session;
import org.osc.core.broker.util.TransactionalBroadcastUtil;

/**
 * Handles broadcasting session messages just after successfull transaction commit
 * or removes session messages from observer in case of transaction logic failure
 */
public class TransactionalBrodcastListener implements TransactionalRunner.TransactionalListener {
    @Override
    public void afterCommit(Session session) {
        TransactionalBroadcastUtil.broadcast(session);
    }

    @Override
    public void afterRollback(Session session) {
        TransactionalBroadcastUtil.removeSessionFromMap(session);
    }
}
