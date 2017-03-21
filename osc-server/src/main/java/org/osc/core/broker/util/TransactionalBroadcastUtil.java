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
import java.util.HashMap;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.view.util.BroadcasterUtil;
import org.osc.core.broker.view.util.EventType;

public class TransactionalBroadcastUtil {

    public volatile static HashMap<EntityManager, ArrayList<BroadcastMessage>> pendingBroadcastMap = new HashMap<>();
    private final static Logger log = Logger.getLogger(TransactionalBroadcastUtil.class);

    public static synchronized void broadcast(EntityManager em) {
        if (pendingBroadcastMap.get(em) != null) {
            try {
                ArrayList<BroadcastMessage> list = pendingBroadcastMap.get(em);
                for (BroadcastMessage msg : list) {
                    log.debug("Broadcasting Message: " + msg.toString());
                    BroadcasterUtil.broadcast(msg);
                }
                // remove session from pendingBroadcastMap
                TransactionalBroadcastUtil.removeSessionFromMap(em);

            } catch (Exception ex) {

                log.error("Broadcasting messages failed", ex);
            }
        }
    }

    public static synchronized void removeSessionFromMap(EntityManager em) {
        log.debug("Removing Session from PendingBraodcastMap");
        try {
            pendingBroadcastMap.remove(em);
        } catch (Exception ex) {
            log.error("Removing Session from PendingBraodcastMap failed", ex);
        }
    }

    public static synchronized void addMessageToMap(EntityManager em, final Long entityId, String receiver,
            EventType eventType) {
        addMessageToMap(em, entityId, receiver, eventType, null);
    }
    public static synchronized void addMessageToMap(EntityManager em, final Long entityId, String receiver,
            EventType eventType, BaseDto dto) {
        BroadcastMessage msg = new BroadcastMessage(entityId, receiver, eventType, dto);
        if (pendingBroadcastMap.get(em) == null) {
            pendingBroadcastMap.put(em, new ArrayList<BroadcastMessage>());
        }
        pendingBroadcastMap.get(em).add(msg);
    }
}
