package org.osc.core.broker.model.entities.archive;

public enum ThresholdType {
    MONTHS("MONTHS"), YEARS("YEARS");

    private final String text;

    ThresholdType(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
