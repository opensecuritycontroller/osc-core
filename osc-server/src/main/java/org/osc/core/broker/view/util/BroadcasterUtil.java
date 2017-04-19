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
package org.osc.core.broker.view.util;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.osc.core.broker.service.broadcast.BroadcastListener;
import org.osc.core.broker.service.broadcast.BroadcastMessage;
import org.osc.core.broker.service.broadcast.Broadcaster;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component
public class BroadcasterUtil implements Broadcaster {

    private ExecutorService executorService;

    private LinkedList<BroadcastListener> listeners = new LinkedList<BroadcastListener>();

    @Activate
    void start() {
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Deactivate
    synchronized void stop() {
        this.executorService.shutdown();
    }

    /**
     * Add given listener to listeners list
     *
     * @param listener
     */
    @Reference(cardinality=ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    synchronized void addListener(BroadcastListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Removes given listener from the list
     *
     * @param listener
     */
    synchronized void removeListener(BroadcastListener listener) {
        this.listeners.remove(listener);
    }

    /**
     * broadcast message to all registered listeners
     *
     * @param entityID
     * @param receiver
     *            (entity class simple name)
     * @param event
     */
    @Override
    public synchronized void broadcast(final BroadcastMessage msg) {
        for (final BroadcastListener listener : this.listeners) {
            this.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    listener.receiveBroadcast(msg);
                }
            });
        }
    }
}