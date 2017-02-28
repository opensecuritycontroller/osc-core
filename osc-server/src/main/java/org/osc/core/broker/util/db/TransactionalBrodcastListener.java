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
