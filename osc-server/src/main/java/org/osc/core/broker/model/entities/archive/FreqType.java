package org.osc.core.broker.model.entities.archive;

public enum FreqType {
    WEEKLY("WEEKLY"), MONTHLY("MONTHLY");

    private final String text;

    FreqType(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
