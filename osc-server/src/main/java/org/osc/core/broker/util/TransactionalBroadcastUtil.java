/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.view.util.BroadcasterUtil;
import org.osc.core.broker.view.util.EventType;

public class TransactionalBroadcastUtil {

    public volatile static HashMap<Session, ArrayList<BroadcastMessage>> pendingBroadcastMap = new HashMap<Session, ArrayList<BroadcastMessage>>();
    private final static Logger log = Logger.getLogger(TransactionalBroadcastUtil.class);

    public static synchronized void broadcast(Session session) {
        if (pendingBroadcastMap.get(session) != null) {
            try {
                ArrayList<BroadcastMessage> list = pendingBroadcastMap.get(session);
                for (BroadcastMessage msg : list) {
                    log.debug("Broadcasting Message: " + msg.toString());
                    BroadcasterUtil.broadcast(msg);
                }
                // remove session from pendingBroadcastMap
                TransactionalBroadcastUtil.removeSessionFromMap(session);

            } catch (Exception ex) {

                log.error("Broadcasting messages failed", ex);
            }
        }
    }

    public static synchronized void removeSessionFromMap(Session session) {
        log.debug("Removing Session from PendingBraodcastMap");
        try {
            pendingBroadcastMap.remove(session);
        } catch (Exception ex) {
            log.error("Removing Session from PendingBraodcastMap failed", ex);
        }
    }

    public static synchronized void addMessageToMap(Session session, final Long entityId, String receiver,
            EventType eventType) {
        addMessageToMap(session, entityId, receiver, eventType, null);
    }
    public static synchronized void addMessageToMap(Session session, final Long entityId, String receiver,
            EventType eventType, BaseDto dto) {
        BroadcastMessage msg = new BroadcastMessage(entityId, receiver, eventType, dto);
        if (pendingBroadcastMap.get(session) == null) {
            pendingBroadcastMap.put(session, new ArrayList<BroadcastMessage>());
        }
        pendingBroadcastMap.get(session).add(msg);
    }
}
