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
package org.osc.core.broker.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.broker.view.util.BroadcasterUtil;
import org.osc.core.broker.view.util.EventType;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionStatus;

public class TransactionalBroadcastUtil {

    private final static Logger log = Logger.getLogger(TransactionalBroadcastUtil.class);

    private static void sendBroadcast(List<BroadcastMessage> messages) {
        try {
            for (BroadcastMessage msg : messages) {
                log.debug("Broadcasting Message: " + msg.toString());
                BroadcasterUtil.broadcast(msg);
            }
        } catch (Exception ex) {
            log.error("Broadcasting messages failed", ex);
        }
    }

    public static synchronized void addMessageToMap(final Long entityId, String receiver,
            EventType eventType) {
        addMessageToMap(entityId, receiver, eventType, null);
    }
    public static synchronized void addMessageToMap(final Long entityId, String receiver,
            EventType eventType, BaseDto dto) {
        BroadcastMessage msg = new BroadcastMessage(entityId, receiver, eventType, dto);

        TransactionControl txControl;
        try {
            txControl = HibernateUtil.getTransactionControl();
        } catch (Exception e) {
            log.error("Unable to acquire the current transaction context", e);
            throw new RuntimeException(e);
        }

        if(txControl.activeScope()) {
            TransactionContext transactionContext = txControl.getCurrentContext();
            @SuppressWarnings("unchecked")
            List<BroadcastMessage> list = (List<BroadcastMessage>) transactionContext
                    .getScopedValue(TransactionalBroadcastUtil.class);

            if(list == null) {
                list = new ArrayList<>();
                transactionContext.putScopedValue(TransactionalBroadcastUtil.class, list);

                // We need an effectively final value for the lambda
                List<BroadcastMessage> forSending = list;
                transactionContext.postCompletion(status -> {
                        if(status == TransactionStatus.COMMITTED) {
                            sendBroadcast(forSending);
                        }
                    });
            }

            list.add(msg);
        } else {
            throw new IllegalStateException("No scope is available to add the BroadcastMessage to");
        }
    }
}
