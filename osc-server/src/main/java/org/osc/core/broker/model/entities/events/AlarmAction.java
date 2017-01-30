package org.osc.core.broker.model.entities.events;

public enum AlarmAction {

    NONE("None"), EMAIL("Email");

    private final String text;

    private AlarmAction(final String text) {
        this.text = text;
    }

    public static AlarmAction fromText(String text) {
        for (AlarmAction action : AlarmAction.values()) {
            if (action.getText().equals(text)) {
                return action;
            }
        }
        throw new IllegalArgumentException("No enum constant " + AlarmAction.class.getCanonicalName() + " Found for " + text);
    }

    private String getText() {
        return this.text;
    }

    @Override
    public String toString() {
        return text;
    }
}
