package org.osc.core.broker.model.entities.events;

public enum EventType {
    JOB_FAILURE("Job Failure"),
    SYSTEM_FAILURE("System Failure"),
    DAI_FAILURE("DAI Failure");

    private final String text;

    private EventType(final String text) {
        this.text = text;
    }

    public static EventType fromText(String text) {
        for (EventType type : EventType.values()) {
            if (type.getText().equals(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum constant " + EventType.class.getCanonicalName() + " Found for "
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
