package org.osc.core.broker.view.util;

public enum EventType {
    ADDED("ADDED"), UPDATED("UPDATED"), DELETED("DELETED");

    private final String text;

    private EventType(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
