package org.osc.core.broker.view.util;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.osc.core.broker.util.BroadcastMessage;

public class BroadcasterUtil implements Serializable {
    private static final long serialVersionUID = 1L;

    private static ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * Needs to implement this interface in order to be able to receive
     * broadcasted messages
     *
     */
    public interface BroadcastListener {
        void receiveBroadcast(BroadcastMessage msg);
    }

    private static LinkedList<BroadcastListener> listeners = new LinkedList<BroadcastListener>();

    /**
     * Add given listener to listeners list
     *
     * @param listener
     */
    public static synchronized void register(BroadcastListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes given listener from the list
     *
     * @param listener
     */
    public static synchronized void unregister(BroadcastListener listener) {
        listeners.remove(listener);
    }

    /**
     * broadcast message to all registered listeners
     *
     * @param entityID
     * @param receiver
     *            (entity class simple name)
     * @param event
     */
    public static synchronized void broadcast(final BroadcastMessage msg) {
        for (final BroadcastListener listener : listeners) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    listener.receiveBroadcast(msg);
                }
            });
        }
    }
}
