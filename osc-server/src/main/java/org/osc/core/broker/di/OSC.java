package org.osc.core.broker.di;

public class OSC {
    private static OSCFactory factory;

    public static void setFactory(OSCFactory objectsFactory) {
        factory = objectsFactory;
    }

    public static OSCFactory get() {
        return factory;
    }
}
