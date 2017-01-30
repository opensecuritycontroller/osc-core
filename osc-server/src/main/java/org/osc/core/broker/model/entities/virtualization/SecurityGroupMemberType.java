package org.osc.core.broker.model.entities.virtualization;

public enum SecurityGroupMemberType {
    VM("VM"), NETWORK("NETWORK"), IP("IP"), MAC("MAC"), SUBNET("SUBNET");

    private final String text;

    private SecurityGroupMemberType(final String text) {
        this.text = text;
    }

    public static SecurityGroupMemberType fromText(String text) {
        for (SecurityGroupMemberType type : SecurityGroupMemberType.values()) {
            if (type.getText().equals(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum constant " + SecurityGroupMemberType.class.getCanonicalName()
                + " Found for " + text);
    }

    private String getText() {
        return this.text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
