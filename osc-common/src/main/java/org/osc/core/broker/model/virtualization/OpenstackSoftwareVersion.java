package org.osc.core.broker.model.virtualization;


public enum OpenstackSoftwareVersion {
    OS_ICEHOUSE("Icehouse");

    private final String text;

    private OpenstackSoftwareVersion(final String text) {
        this.text = text;
    }

    public static OpenstackSoftwareVersion fromText(String text) {
        for (OpenstackSoftwareVersion version : OpenstackSoftwareVersion.values()) {
            if (version.getText().equals(text)) {
                return version;
            }
        }
        throw new IllegalArgumentException("No enum constant " + OpenstackSoftwareVersion.class.getCanonicalName() + " Found for " + text);
    }

    private String getText() {
        return this.text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
