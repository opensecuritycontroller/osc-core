package org.osc.core.broker.model.entities.events;

public enum Severity {

    HIGH("High"), MEDIUM("Medium"), LOW("Low");

    private final String text;

    private Severity(final String text) {
        this.text = text;
    }

    public static Severity fromText(String text) {
        for (Severity severity : Severity.values()) {
            if (severity.getText().equals(text)) {
                return severity;
            }
        }
        throw new IllegalArgumentException("No enum constant " + Severity.class.getCanonicalName() + " Found for "
                + text);
    }

    private String getText() {
        return this.text;
    }

    @Override
    public String toString() {
        return text;
    }
}
