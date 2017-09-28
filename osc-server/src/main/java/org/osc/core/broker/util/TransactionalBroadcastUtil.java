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

import org.osc.core.broker.service.broadcast.BroadcastMessage;
import org.osc.core.broker.service.broadcast.Broadcaster;
import org.osc.core.broker.service.broadcast.EventType;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.transaction.control.TransactionContext;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.TransactionStatus;
import org.slf4j.Logger;

@Component(service=TransactionalBroadcastUtil.class)
public class TransactionalBroadcastUtil {

    @Reference
    Broadcaster broadcaster;

    @Reference
    DBConnectionManager dbConnectionManager;

    private final static Logger log = LoggerFactory.getLogger(TransactionalBroadcastUtil.class);

    private void sendBroadcast(List<BroadcastMessage> messages) {
        try {
            for (BroadcastMessage msg : messages) {
                log.debug("Broadcasting Message: " + msg.toString());
                this.broadcaster.broadcast(msg);
            }
        } catch (Exception ex) {
            log.error("Broadcasting messages failed", ex);
        }
    }

    public synchronized void addMessageToMap(final Long entityId, String receiver,
            EventType eventType) {
        addMessageToMap(entityId, receiver, eventType, null);
    }

    public synchronized void addMessageToMap(final Long entityId, String receiver,
            EventType eventType, BaseDto dto) {
        BroadcastMessage msg = new BroadcastMessage(entityId, receiver, eventType, dto);

        TransactionControl txControl;
        try {
            txControl = this.dbConnectionManager.getTransactionControl();
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
