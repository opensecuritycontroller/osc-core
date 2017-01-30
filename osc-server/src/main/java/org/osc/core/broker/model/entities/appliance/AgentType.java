package org.osc.core.broker.model.entities.appliance;

public enum AgentType {
    AGENTLESS("Agentless"),
    AGENT("Agent");

    private final String text;

    private AgentType(final String text) {
        this.text = text;
    }

    public static AgentType fromText(String text) {
        for (AgentType type : AgentType.values()) {
            if (type.getText().equals(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum constant " + AgentType.class.getCanonicalName() + " Found for "
                + text);
    }

    private String getText() {
        return this.text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}



