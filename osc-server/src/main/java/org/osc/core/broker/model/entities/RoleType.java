package org.osc.core.broker.model.entities;

public enum RoleType {
    ADMIN("ADMIN"), SYSTEM_AGENT("SYSTEM_AGENT"), SYSTEM_NSX("SYSTEM_NSX");

    private final String text;

    RoleType(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
